package cn.bugstack.trigger.listener;
import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import cn.bugstack.domain.activity.service.IRaffleOrderAccountQuoat;
import cn.bugstack.domain.credit.model.entity.TradeEntity;
import cn.bugstack.domain.credit.model.valobj.TradeNameVO;
import cn.bugstack.domain.credit.model.valobj.TradeTypeVO;
import cn.bugstack.domain.credit.service.ICreditAdjustService;
import cn.bugstack.domain.rebate.event.SendRebateMessageEvent;
import cn.bugstack.domain.rebate.model.valobj.BehaviorRebateTypeVO;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.event.BaseEvent;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@Component
public class BehaviorRebateServiceTaskConsumer {
    @Value("${spring.rabbitmq.topic.send_rebate}")
    private String topic;
    @Resource
    private IRaffleOrderAccountQuoat iRaffleOrderAccountQuoat;
    @Resource
    private ICreditAdjustService creditAdjustService;
    @RabbitListener(queuesToDeclare = @Queue(value = "send_rebate"))
    public void listener(String message) {
        try{
            log.info("监听返利消息topic:{},message:{}", topic, message);
            BaseEvent.EventMessage<SendRebateMessageEvent.SendRebateMessage> eventMessage
                    = JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<SendRebateMessageEvent.SendRebateMessage>>() {
            }.getType());
            SendRebateMessageEvent.SendRebateMessage sendRebateMessage = eventMessage.getData();
            // 2. 入账奖励
            switch (sendRebateMessage.getRebateType()) {
                case "sku":
                    SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
                    skuRechargeEntity.setUserId(sendRebateMessage.getUserId());
                    skuRechargeEntity.setSku(Long.valueOf(sendRebateMessage.getRebateConfig()));
                    skuRechargeEntity.setOutBusinessNo(sendRebateMessage.getBizId());
                    iRaffleOrderAccountQuoat.createSkuRechargeOrder(skuRechargeEntity);
                    break;
                case "integral":
                    TradeEntity tradeEntity = new TradeEntity();
                    tradeEntity.setUserId(sendRebateMessage.getUserId());
                    tradeEntity.setTradeName(TradeNameVO.REBATE);
                    tradeEntity.setTradeType(TradeTypeVO.FORWARD);
                    tradeEntity.setAmount(new BigDecimal(sendRebateMessage.getRebateConfig()));
                    tradeEntity.setOutBusinessNo(sendRebateMessage.getBizId());
                    creditAdjustService.createOrder(tradeEntity);
                    break;
            }
        }catch (AppException e) {
            if (ResponseCode.INDEX_DUP.getCode().equals(e.getCode())) {
                log.warn("监听用户行为返利消息，消费重复 topic: {} message: {}", topic, message, e);
                return;
            }
            throw e;
        } catch (Exception e) {
            log.error("监听用户行为返利消息，消费失败 topic: {} message: {}", topic, message, e);
            throw e;
        }

    }
}
