package cn.bugstack.domain.rebate.service;

import cn.bugstack.domain.award.model.valobj.TaskSendStateVO;
import cn.bugstack.domain.rebate.event.SendRebateMessageEvent;
import cn.bugstack.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import cn.bugstack.domain.rebate.model.entity.BehaviorEntity;
import cn.bugstack.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.bugstack.domain.rebate.model.entity.TaskEntity;
import cn.bugstack.domain.rebate.model.valobj.DailyBehaviorRebateVO;
import cn.bugstack.domain.rebate.respository.IBehaviorRebateRespository;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.event.BaseEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BehaviorRebateService implements IBehaviorRebateService {
    @Resource
    IBehaviorRebateRespository iBehaviorRebateRespository;
    @Resource
    SendRebateMessageEvent sendRebateMessageEvent;

    @Override
    public List<String> createOrder(BehaviorEntity behaviorEntity) {
        List<DailyBehaviorRebateVO> dailyBehaviorRebateVOS = iBehaviorRebateRespository.queryDailyBehaviorRebateConfig(behaviorEntity.getBehaviorTypeVO());
        if (dailyBehaviorRebateVOS == null || dailyBehaviorRebateVOS.isEmpty()) {
            return new ArrayList<>();
        }
        // 2. 构建聚合对象
        List<String> orderIds = new ArrayList<>();
        List<BehaviorRebateAggregate> behaviorRebateAggregates = new ArrayList<>();
        for (DailyBehaviorRebateVO dailyBehaviorRebateVO : dailyBehaviorRebateVOS) {
            // 拼装业务ID；用户ID_返利类型_外部透彻业务ID
            String bizId = behaviorEntity.getUserId() + Constants.UNDERLINE + dailyBehaviorRebateVO.getRebateType() + Constants.UNDERLINE + behaviorEntity.getOutBusinessNo();
            BehaviorRebateOrderEntity behaviorRebateOrderEntity = BehaviorRebateOrderEntity.builder()
                    .userId(behaviorEntity.getUserId())
                    .orderId(RandomStringUtils.randomNumeric(12))
                    .behaviorType(dailyBehaviorRebateVO.getBehaviorType())
                    .rebateDesc(dailyBehaviorRebateVO.getRebateDesc())
                    .rebateType(dailyBehaviorRebateVO.getRebateType())
                    .rebateConfig(dailyBehaviorRebateVO.getRebateConfig())
                    .bizId(bizId)
                    .build();
            orderIds.add(behaviorRebateOrderEntity.getOrderId());
            //MQ消息
            SendRebateMessageEvent.SendRebateMessage rebateMessage = SendRebateMessageEvent.SendRebateMessage.builder()
                    .userId(behaviorEntity.getUserId())
                    .rebateType(dailyBehaviorRebateVO.getRebateType())
                    .rebateDesc(dailyBehaviorRebateVO.getRebateDesc())
                    .rebateConfig(dailyBehaviorRebateVO.getRebateConfig())
                    .bizId(bizId)
                    .build();
            //构建消息
            BaseEvent.EventMessage<SendRebateMessageEvent.SendRebateMessage> rebateMessageEventMessage = sendRebateMessageEvent
                    .buildEventMessage(rebateMessage);
            //组装任务对象
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setUserId(behaviorEntity.getUserId());
            taskEntity.setMessage(rebateMessageEventMessage);
            taskEntity.setTopic(sendRebateMessageEvent.topic());
            taskEntity.setState(TaskSendStateVO.create);
            taskEntity.setMessageId(rebateMessageEventMessage.getId());
            //聚合对象
            BehaviorRebateAggregate behaviorRebateAggregate = new BehaviorRebateAggregate();
            behaviorRebateAggregate.setUserId(behaviorEntity.getUserId());
            behaviorRebateAggregate.setBehaviorRebateOrderEntity(behaviorRebateOrderEntity);
            behaviorRebateAggregate.setTaskEntity(taskEntity);
            behaviorRebateAggregates.add(behaviorRebateAggregate);
        }

        //存储聚合对象数据
        iBehaviorRebateRespository.saveUserRebateRecord(behaviorEntity.getUserId(), behaviorRebateAggregates);
        //返回订单集合
        return orderIds;
    }
}
