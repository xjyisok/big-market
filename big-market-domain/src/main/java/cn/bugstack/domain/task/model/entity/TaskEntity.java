package cn.bugstack.domain.task.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskEntity {
    private String userId;
    private String topic;
    private String messageId;
    private String message;
}
