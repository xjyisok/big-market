package cn.bugstack.domain.rebate.event;

import cn.bugstack.domain.award.event.SendAwardMessageEvent;
import cn.bugstack.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SendRebateMessageEvent extends BaseEvent<SendRebateMessageEvent.SendRebateMessage> {
    @Value("${spring.rabbitmq.topic.send_rebate}")
    private String topic;
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SendRebateMessage {
        /**
         * 用户Id
         */
        private String userId;
        /** 返利描述 */
        private String rebateDesc;
        /** 返利类型（sku 活动库存充值商品、integral 用户活动积分） */
        private String rebateType;
        /** 返利配置【sku值，积分值】 */
        private String rebateConfig;
        /** 业务ID - 拼接的唯一值 */
        private String bizId;
    }

    @Override
    public BaseEvent.EventMessage<SendRebateMessageEvent.SendRebateMessage> buildEventMessage(SendRebateMessageEvent.SendRebateMessage data) {
        return BaseEvent.EventMessage.<SendRebateMessageEvent.SendRebateMessage>builder()
                .id(RandomStringUtils.randomNumeric(11))
                .timestamp(new Date())
                .data(data)
                .build();
    }

    @Override
    public String topic() {
        return topic;
    }
}
