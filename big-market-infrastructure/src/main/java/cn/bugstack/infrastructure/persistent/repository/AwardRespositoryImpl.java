package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.bugstack.domain.award.model.entity.TaskEntity;
import cn.bugstack.domain.award.model.entity.UserAwardRecordEntity;
import cn.bugstack.domain.award.model.valobj.AwardSendStateVO;
import cn.bugstack.domain.award.respository.IAwardrespository;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.infrastructure.persistent.dao.ITaskDao;
import cn.bugstack.infrastructure.persistent.dao.IUserAwardRecordDao;
import cn.bugstack.infrastructure.persistent.po.Task;
import cn.bugstack.infrastructure.persistent.po.UserAwardRecord;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.transaction.Transaction;
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
    @Override
    public void saveUserAwardRecordAggerate(UserAwardRecordAggregate userAwardRecordAggregate) {
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

        try{
            dbrouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    userAwardRecordDao.insert(userAwardRecord);
                    taskDao.insert(task);
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
}
