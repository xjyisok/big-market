package cn.bugstack.domain.activity.model.entity;

import cn.bugstack.domain.activity.model.valobj.OrderTradeTypeVO;
import lombok.Data;

@Data
public class SkuRechargeEntity {
    private String userId;
    private Long sku;
    private String outBusinessNo;
    private OrderTradeTypeVO orderTradeType=OrderTradeTypeVO.rebate_no_pay_trade;
}
