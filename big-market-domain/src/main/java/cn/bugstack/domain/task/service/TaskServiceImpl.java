package cn.bugstack.domain.task.service;

import cn.bugstack.domain.task.model.entity.TaskEntity;
import cn.bugstack.domain.task.respository.ITaskRespository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
@Service
public class TaskServiceImpl implements ITaskService {
    @Resource
    ITaskRespository taskRespository;
    @Override
    public List<TaskEntity> queryNoSendMessageTaskList() {
        return taskRespository.queryNoSendMessageTaskList();
    }

    @Override
    public void sendMessage(TaskEntity taskEntity) {
        taskRespository.sendMessage(taskEntity);
    }

    @Override
    public void updateTaskSendMessageCompleted(String userId, String messageId) {
        taskRespository.updateTaskSendMessageCompleted(userId, messageId);
    }

    @Override
    public void updateTaskSendMessageFail(String userId, String messageId) {
        taskRespository.updateTaskSendMessageFail(userId, messageId);
    }
}
