package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.credit.model.aggergate.TradeAggregate;
import cn.bugstack.domain.credit.model.entity.CreditAccountEntity;
import cn.bugstack.domain.credit.model.entity.CreditOrderEntity;
import cn.bugstack.domain.credit.model.entity.TaskEntity;
import cn.bugstack.domain.credit.repository.ICreditRepository;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.infrastructure.persistent.dao.ITaskDao;
import cn.bugstack.infrastructure.persistent.dao.IUserCreditAccountDao;
import cn.bugstack.infrastructure.persistent.dao.IUserCreditOrderDao;
import cn.bugstack.infrastructure.persistent.po.Task;
import cn.bugstack.infrastructure.persistent.po.UserCreditAccount;
import cn.bugstack.infrastructure.persistent.po.UserCreditOrder;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.common.Constants;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
public class CreditRepositoryImpl implements ICreditRepository {
    @Resource
    private IRedisService redisService;
    @Resource
    private IUserCreditOrderDao userCreditOrderDao;
    @Resource
    private IUserCreditAccountDao userCreditAccountDao;
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    EventPublisher eventPublisher;
    @Resource
    ITaskDao taskDao;
    @Override
    public void saveUserCreditTradeOrder(TradeAggregate tradeAggregate) {
        String userId = tradeAggregate.getUserId();
        CreditAccountEntity creditAccountEntity = tradeAggregate.getCreditAccountEntity();
        CreditOrderEntity creditOrderEntity = tradeAggregate.getCreditOrderEntity();
        TaskEntity taskEntity = tradeAggregate.getTaskEntity();
        //创建账户
        UserCreditAccount userCreditAccount=new UserCreditAccount();
        userCreditAccount.setUserId(userId);
        userCreditAccount.setTotalAmount(creditAccountEntity.getAdjustAmount());
        userCreditAccount.setAvailableAmount(creditAccountEntity.getAdjustAmount());
        //创建持久化订单实体
        UserCreditOrder userCreditOrder=new UserCreditOrder();
        userCreditOrder.setUserId(userId);
        userCreditOrder.setOrderId(creditOrderEntity.getOrderId());
        userCreditOrder.setTradeAmount(creditOrderEntity.getTradeAmount());
        userCreditOrder.setTradeName(creditOrderEntity.getTradeName().name());
        userCreditOrder.setTradeType(creditOrderEntity.getTradeType().name());
        userCreditOrder.setOutBusinessNo(creditOrderEntity.getOutBusinessNo());
        //创建任务实体
        Task task = new Task();
        task.setUserId(taskEntity.getUserId());
        task.setTopic(taskEntity.getTopic());
        task.setMessageId(taskEntity.getMessageId());
        task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
        task.setState(taskEntity.getState().getCode());

        RLock lock=redisService.getLock(Constants.RedisKey.USER_CREDIT_ACCOUNT_LOCK+userId+Constants.UNDERLINE+creditOrderEntity.getOrderId());
        try{
            lock.lock(3, TimeUnit.SECONDS);
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try{
                    UserCreditAccount usercreditAccountRes=userCreditAccountDao.queryUserCreditAccount(userCreditAccount);
                    if(usercreditAccountRes==null){
                        userCreditAccountDao.insert(userCreditAccount);
                    }else {
                        userCreditAccountDao.updateAddAmount(userCreditAccount);
                    }
                    userCreditOrderDao.insert(userCreditOrder);
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("调整账户积分额度异常，唯一索引冲突userId:{} orderId:{}", userId, creditOrderEntity.getOrderId());
                }
                catch(Exception e){
                    status.setRollbackOnly();
                    log.error("调整账户积分额度失败 userId:{} orderId:{}", userId, creditOrderEntity.getOrderId(),e);
                }
                return 1;
            });
        }finally{
            lock.unlock();
            dbRouter.clear();
        }
        try {
            // 发送消息【在事务外执行，如果失败还有任务补偿】
            eventPublisher.publish(task.getTopic(), task.getMessage());
            // 更新数据库记录，task 任务表
            taskDao.updateTaskSendMessageCompleted(task);
            log.info("调整账户积分记录，发送MQ消息完成 userId: {} orderId:{} topic: {}", userId, creditOrderEntity.getOrderId(), task.getTopic());
        } catch (Exception e) {
            log.error("调整账户积分记录，发送MQ消息失败 userId: {} topic: {}", userId, task.getTopic());
            taskDao.updateTaskSendMessageFail(task);
        }


    }
}
