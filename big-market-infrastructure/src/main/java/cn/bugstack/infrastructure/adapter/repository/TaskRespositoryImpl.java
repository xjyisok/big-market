package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.task.model.entity.TaskEntity;
import cn.bugstack.domain.task.respository.ITaskRespository;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.infrastructure.dao.ITaskDao;
import cn.bugstack.infrastructure.dao.po.Task;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TaskRespositoryImpl implements ITaskRespository {
    @Resource
    private ITaskDao taskDao;
    @Resource
    private EventPublisher eventPublisher;

    @Override
    public List<TaskEntity> queryNoSendMessageTaskList() {
        List<Task>taskList = taskDao.queryNoSendMessageTaskList();
        List<TaskEntity>taskEntityList = new ArrayList<TaskEntity>(taskList.size());
        for (Task task : taskList) {
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setMessage(task.getMessage());
            taskEntity.setMessageId(task.getMessageId());
            taskEntity.setUserId(task.getUserId());
            taskEntity.setTopic(task.getTopic());
            taskEntityList.add(taskEntity);
        }
        return taskEntityList;
    }

    @Override
    public void sendMessage(TaskEntity taskEntity) {
        eventPublisher.publish(taskEntity.getTopic(), taskEntity.getMessage());
    }

    @Override
    public void updateTaskSendMessageCompleted(String userId, String messageId) {
        Task task = new Task();
        task.setUserId(userId);
        task.setMessageId(messageId);
        taskDao.updateTaskSendMessageCompleted(task);
    }

    @Override
    public void updateTaskSendMessageFail(String userId, String messageId) {
        Task task = new Task();
        task.setUserId(userId);
        task.setMessageId(messageId);
        taskDao.updateTaskSendMessageFail(task);
    }
}
