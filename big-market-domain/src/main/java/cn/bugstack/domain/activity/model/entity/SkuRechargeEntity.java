package cn.bugstack.domain.activity.model.entity;

import lombok.Data;

@Data
public class SkuRechargeEntity {
    private String userId;
    private Long sku;
    private String outBusinessNo;
}
