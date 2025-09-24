package cn.bugstack.domain.award.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AwardSendStateVO {
     create("create","奖品发放任务创建"),
     complete("complete","奖品发放任务完成"),
     failed("failed","奖品发放失败");
     private final String code;
     private final String info;
}
