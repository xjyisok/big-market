package cn.bugstack.domain.strategy.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RuleLogicCheckTypeVO {
    ALLOW("0000","放行；执行后续规则不受规则引擎影响"),

    TAKE_OVER("0001","接管后续流程受规则引擎执行结果影响"),
    ;

    private String code;
    private String info;
}
