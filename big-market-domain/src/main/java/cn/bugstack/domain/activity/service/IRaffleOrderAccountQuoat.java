package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.*;

public interface IRaffleOrderAccountQuoat {
    public ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);
    public UnpaidActivityOrderEntity createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity);
    public ActivityAccountEntity queryUserActivityAccount(String userId, Long activityId);

    Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId);
    void updateOrder(DeliveryOrderEntity deliveryOrderEntity);
}
