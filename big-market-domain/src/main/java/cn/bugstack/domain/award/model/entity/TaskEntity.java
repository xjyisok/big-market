package cn.bugstack.domain.award.model.entity;

import cn.bugstack.domain.award.event.SendAwardMessageEvent;
import cn.bugstack.domain.award.model.valobj.TaskSendStateVO;
import cn.bugstack.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskEntity {
    /**用户id*/
    private String userId;
    /** 消息主题 */
    private String topic;
    /**消息Id*/
    private String messageId;
    /** 消息主体 */
    private BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> message;
    /** 任务状态；create-创建、completed-完成、fail-失败 */
    private TaskSendStateVO state;
}
