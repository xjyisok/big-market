package cn.bugstack.domain.activity.model.entity;

import cn.bugstack.domain.activity.model.valobj.OrderTradeTypeVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuRechargeEntity {
    private String userId;
    private Long sku;
    private String outBusinessNo;
    private OrderTradeTypeVO orderTradeType=OrderTradeTypeVO.rebate_no_pay_trade;
}
