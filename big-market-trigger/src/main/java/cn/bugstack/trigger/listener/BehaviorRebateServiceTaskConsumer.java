package cn.bugstack.trigger.listener;
import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import cn.bugstack.domain.activity.service.IRaffleOrderAccountQuoat;
import cn.bugstack.domain.rebate.event.SendRebateMessageEvent;
import cn.bugstack.domain.rebate.model.valobj.BehaviorRebateTypeVO;
import cn.bugstack.types.event.BaseEvent;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class BehaviorRebateServiceTaskConsumer {
    @Value("${spring.rabbitmq.topic.send_rebate}")
    private String topic;
    @Resource
    private IRaffleOrderAccountQuoat iRaffleOrderAccountQuoat;
    @RabbitListener(queuesToDeclare = @Queue(value = "send_rebate"))
    public void listener(String message) {
        try{
            log.info("监听返利消息topic:{},message:{}", topic, message);
            BaseEvent.EventMessage<SendRebateMessageEvent.SendRebateMessage> eventMessage
                    = JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<SendRebateMessageEvent.SendRebateMessage>>() {
            }.getType());
            SendRebateMessageEvent.SendRebateMessage sendRebateMessage = eventMessage.getData();
            if(!sendRebateMessage.getRebateType().equals(BehaviorRebateTypeVO.SKU.getCode())){
                log.info("非sku任务暂时不处理topic:{},message:{}", topic, message);
                return;
            }
            SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
            skuRechargeEntity.setSku(Long.parseLong(sendRebateMessage.getRebateConfig()));
            skuRechargeEntity.setUserId(sendRebateMessage.getUserId());
            skuRechargeEntity.setOutBusinessNo(sendRebateMessage.getBizId());
            iRaffleOrderAccountQuoat.createSkuRechargeOrder(skuRechargeEntity);
        }catch (Exception e){
            log.error("监听用户行为返利消息，消费失败topic:{} message:{}",topic,message, e);
        }
    }
}
