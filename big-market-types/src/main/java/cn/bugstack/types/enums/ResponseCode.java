package cn.bugstack.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    STRATEGY_RULE_WEIGHT_IS_NULL("ERR_BIZ_001","业务异常，策略规则中rule_weight权重规则已使用但未配置"),
    UN_ASSEMBLED_STRATEGY_ARMORY("ERR_BIZ_001","抽奖策略配置未装配，请使用IStrategyArmory完成装配"),
    INDEX_DUP("0003","唯一索引异常")
    ;

    private String code;
    private String info;

}
