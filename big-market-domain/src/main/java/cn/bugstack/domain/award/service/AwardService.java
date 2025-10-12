package cn.bugstack.domain.award.service;

import cn.bugstack.domain.award.event.SendAwardMessageEvent;
import cn.bugstack.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.bugstack.domain.award.model.entity.DistributeAwardEntity;
import cn.bugstack.domain.award.model.entity.TaskEntity;
import cn.bugstack.domain.award.model.entity.UserAwardRecordEntity;
import cn.bugstack.domain.award.model.valobj.TaskSendStateVO;
import cn.bugstack.domain.award.respository.IAwardrespository;
import cn.bugstack.domain.award.service.distribute.IDistributeAward;
import cn.bugstack.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Service
public class AwardService implements IAwardService {
    //@Resource
    private IAwardrespository awardrespository;
    //@Resource
    private SendAwardMessageEvent sendAwardMessageEvent;
    private final Map<String, IDistributeAward>distributeAwardMap;

    public AwardService(Map<String, IDistributeAward> distributeAwardMap, IAwardrespository awardrespository,
                        SendAwardMessageEvent sendAwardMessageEvent) {
        this.distributeAwardMap = distributeAwardMap;
        this.awardrespository = awardrespository;
        this.sendAwardMessageEvent = sendAwardMessageEvent;
    }

    @Override
    public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {
        //构建MQ消息对象
        SendAwardMessageEvent.SendAwardMessage sendAwardMessage = new SendAwardMessageEvent.SendAwardMessage();
        sendAwardMessage.setAwardId(userAwardRecordEntity.getAwardId());
        sendAwardMessage.setAwardTitle(userAwardRecordEntity.getAwardTitle());
        sendAwardMessage.setUserId(userAwardRecordEntity.getUserId());
        sendAwardMessage.setOrderId(userAwardRecordEntity.getOrderId());
        sendAwardMessage.setAwardConfig(userAwardRecordEntity.getAwardConfig());
        BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> sendAwardMessageEventMessage=sendAwardMessageEvent.buildEventMessage(sendAwardMessage);
        //构建Task对象
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUserId(userAwardRecordEntity.getUserId());
        taskEntity.setTopic(sendAwardMessageEvent.topic());
        taskEntity.setMessageId(sendAwardMessageEventMessage.getId());
        taskEntity.setMessage(sendAwardMessageEventMessage);
        taskEntity.setState(TaskSendStateVO.create);
        //构建聚合对象
        UserAwardRecordAggregate userAwardRecordAggregate = new UserAwardRecordAggregate();
        userAwardRecordAggregate.setUserAwardRecordEntity(userAwardRecordEntity);
        userAwardRecordAggregate.setTaskEntity(taskEntity);
        //保存聚合对象中的元素到库中
        awardrespository.saveUserAwardRecordAggerate(userAwardRecordAggregate);
    }

    @Override
    public void distributeAward(DistributeAwardEntity distributeAwardEntity) {
        String awardKey=awardrespository.queryAwardKey(distributeAwardEntity.getAwardId());
        if(awardKey==null){
            log.error("分发奖品，奖品Id不存在 awardId:{}",distributeAwardEntity.getAwardId());
            return;
        }
        IDistributeAward awardDistributeService=distributeAwardMap.get(awardKey);
        if(awardDistributeService==null){
            log.error("分发奖品，该奖品的分发服务不存在 awardId:{}",distributeAwardEntity.getAwardId());
            return;
        }
        awardDistributeService.giveOutPrizes(distributeAwardEntity);
    }
}
