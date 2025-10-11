package cn.bugstack.domain.rebate.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BehaviorRebateTypeVO {
    SKU("sku", "活动库存充值返利"),
    INTEGRAL("integral", "签到积分"),
    ;

    private final String code;
    private final String info;
}
