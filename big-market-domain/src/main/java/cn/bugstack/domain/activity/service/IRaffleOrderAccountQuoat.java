package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.ActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.ActivityShopCartEntity;
import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;

public interface IRaffleOrderAccountQuoat {
    public ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);
    public String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity);
}
