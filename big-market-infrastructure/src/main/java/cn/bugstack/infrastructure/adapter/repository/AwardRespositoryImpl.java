package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.award.model.aggregate.GiveOutPrizesAggregate;
import cn.bugstack.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.bugstack.domain.award.model.entity.TaskEntity;
import cn.bugstack.domain.award.model.entity.UserAwardRecordEntity;
import cn.bugstack.domain.award.model.entity.UserCreditAwardEntity;
import cn.bugstack.domain.award.model.valobj.AccountStatusVO;
import cn.bugstack.domain.award.respository.IAwardrespository;
import cn.bugstack.infrastructure.dao.*;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.infrastructure.dao.po.Task;
import cn.bugstack.infrastructure.dao.po.UserAwardRecord;
import cn.bugstack.infrastructure.dao.po.UserCreditAccount;
import cn.bugstack.infrastructure.dao.po.UserRaffleOrder;
import cn.bugstack.middleware.db.router.DBContextHolder;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

@Slf4j
@Repository
public class AwardRespositoryImpl implements IAwardrespository {
    @Resource
    IUserAwardRecordDao userAwardRecordDao;
    @Resource
    ITaskDao taskDao;
    @Resource
    IDBRouterStrategy dbrouter;
    @Resource
    TransactionTemplate transactionTemplate;
    @Resource
    EventPublisher eventPublisher;
    @Resource
    IUserRaffleOrderDao userRaffleOrderDao;
    @Resource
    IAwardDao awardDao;
    @Resource
    IUserCreditAccountDao userCreditAccountDao;
    @Override
    public void
    saveUserAwardRecordAggerate(UserAwardRecordAggregate userAwardRecordAggregate) {
        TaskEntity taskEntity = userAwardRecordAggregate.getTaskEntity();
        UserAwardRecordEntity userAwardRecordEntity = userAwardRecordAggregate.getUserAwardRecordEntity();
        String userId=userAwardRecordEntity.getUserId();
        Long activityId=userAwardRecordEntity.getActivityId();
        Integer awardId=userAwardRecordEntity.getAwardId();
        UserAwardRecord userAwardRecord = new UserAwardRecord();
        userAwardRecord.setUserId(userId);
        userAwardRecord.setActivityId(activityId);
        userAwardRecord.setAwardId(awardId);
        userAwardRecord.setStrategyId(userAwardRecordEntity.getStrategyId());
        userAwardRecord.setOrderId(userAwardRecordEntity.getOrderId());
        userAwardRecord.setAwardTitle(userAwardRecordEntity.getAwardTitle());
        userAwardRecord.setAwardTime(userAwardRecordEntity.getAwardTime());
        userAwardRecord.setAwardState(userAwardRecordEntity.getAwardState().getCode());

        Task task=new Task();
        task.setUserId(userId);
        task.setTopic(taskEntity.getTopic());
        task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
        task.setMessageId(taskEntity.getMessageId());
        task.setState(taskEntity.getState().getCode());

        UserRaffleOrder userRaffleOrder=new UserRaffleOrder();
        userRaffleOrder.setUserId(userId);
        userRaffleOrder.setOrderId(userAwardRecordEntity.getOrderId());
        try{
            dbrouter.doRouter(userId);
            String dbKey = DBContextHolder.getDBKey(); // 看你框架里的上下文存的是什么
            log.info("当前路由到的库：{}", dbKey);
            transactionTemplate.execute(status -> {
                try {
                    //写任务
                    userAwardRecordDao.insert(userAwardRecord);
                    //写抽奖单
                    taskDao.insert(task);
                    //更新抽奖订单状态
                    int count=userRaffleOrderDao.updateUserRaffleOrderState(userRaffleOrder);
                    if(count!=1){
                        status.setRollbackOnly();
                        log.error("抽奖单已经使用，不可重复抽奖 userId:{},activtiyId:{}", userId, activityId);
                        throw new AppException(ResponseCode.ACTIVITY_ORDER_ERROR.getCode(),ResponseCode.ACTIVITY_ORDER_ERROR.getInfo());
                    }
                    return 1;
                }catch (DuplicateKeyException e){
                    status.setRollbackOnly();
                    log.error("写入中奖记录唯一索引冲突 userId:{},activtiyId:{}", userId, activityId);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(),ResponseCode.INDEX_DUP.getInfo());
                }
            });
        }finally {
            dbrouter.clear();
        }
        //TODO优化点线程池
        try{
            eventPublisher.publish(task.getTopic(), task.getMessage());
            taskDao.updateTaskSendMessageCompleted(task);
        }catch (Exception e){
            log.error("写入中奖记录发送MQ消息失败 userId:{},activtiyId:{}", userId, activityId);
            taskDao.updateTaskSendMessageFail(task);
        }
    }

    @Override
    public String queryAwardConfig(Integer awardId) {
        return awardDao.queryAwardConfigByAwardId(awardId);
    }

    @Override
    public void saveGiveOutPrizesAggregate(GiveOutPrizesAggregate giveOutPrizesAggregate) {
        String userId=giveOutPrizesAggregate.getUserId();
        UserAwardRecordEntity userAwardRecordEntity=giveOutPrizesAggregate.getUserAwardRecordEntity();
        UserCreditAwardEntity userCreditAwardEntity=giveOutPrizesAggregate.getUserCreditAwardEntity();
        //更新发奖记录
        UserAwardRecord userAwardRecord=new UserAwardRecord();
        userAwardRecord.setUserId(userId);
        userAwardRecord.setAwardId(userAwardRecordEntity.getAwardId());
        userAwardRecord.setOrderId(userAwardRecordEntity.getOrderId());
        //更新用户积分
        UserCreditAccount userCreditAccount=new UserCreditAccount();
        userCreditAccount.setUserId(userId);
        userCreditAccount.setAvailableAmount(userCreditAwardEntity.getCreditAmount());
        userCreditAccount.setTotalAmount(userCreditAwardEntity.getCreditAmount());
        userCreditAccount.setAccountStatus(AccountStatusVO.open.getCode());
        try{
            dbrouter.doRouter(userId);
            //String dbKey = DBContextHolder.getDBKey(); // 看你框架里的上下文存的是什么
            //log.info("当前路由到的库：{}", dbKey);
            transactionTemplate.execute(status -> {
                try {
                    int updateAccountCount=userCreditAccountDao.updateAddAmount(userCreditAccount);
                    if(updateAccountCount==0){
                        userCreditAccountDao.insert(userCreditAccount);
                    }
                    //更新奖品状态记录
                    int updateAwardCount=userAwardRecordDao.updateAwardRecordCompletedState(userAwardRecord);
                    if(updateAwardCount==0){
                        log.warn("更新中奖记录，重复更新拦截 userId:{} giveoutPrizesAggregate:{}", userId, JSON.toJSONString(giveOutPrizesAggregate));
                    }
                    return 1;
                }catch (DuplicateKeyException e){
                    status.setRollbackOnly();
                    log.error("写入中奖记录唯一索引冲突 userId:{}", userId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(),ResponseCode.INDEX_DUP.getInfo());
                }
            });
        }finally {
            dbrouter.clear();
        }
    }

    @Override
    public String queryAwardKey(Integer awardId) {
        return awardDao.queryAwardKeyByAwardId(awardId);
    }
}
