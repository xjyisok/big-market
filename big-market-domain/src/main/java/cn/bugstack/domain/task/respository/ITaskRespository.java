package cn.bugstack.domain.task.respository;

import cn.bugstack.domain.task.model.entity.TaskEntity;

import java.util.List;

public interface ITaskRespository {
    List<TaskEntity> queryNoSendMessageTaskList();

    void sendMessage(TaskEntity taskEntity);

    void updateTaskSendMessageCompleted(String userId, String messageId);

    void updateTaskSendMessageFail(String userId, String messageId);


}
