package cn.bugstack.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStateVO {
    complete("complete","完成"),
    wait_pay("wait_pay","待支付");


    private final String code;
    private final String desc;
}
