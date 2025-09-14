package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.ActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.ActivityShopCartEntity;
import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import org.springframework.stereotype.Service;

public interface IRaffleOrder {
    public ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);
    public String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity);
}
