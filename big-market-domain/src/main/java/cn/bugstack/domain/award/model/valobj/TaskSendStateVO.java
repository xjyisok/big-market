package cn.bugstack.domain.award.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskSendStateVO {
    create("create","MQ任务创建"),
    complete("complete","MQ任务完成"),
    failed("failed","MQ任务失败");
    private final String code;
    private final String info;
}
