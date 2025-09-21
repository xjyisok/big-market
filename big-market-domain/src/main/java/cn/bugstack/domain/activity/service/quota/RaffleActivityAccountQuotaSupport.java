package cn.bugstack.domain.activity.service.quota;

import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.respository.IActivityRespository;
import cn.bugstack.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;

public class RaffleActivityAccountQuotaSupport {
    protected DefaultActivityChainFactory defaultActivityChainFactory;
    protected IActivityRespository activityRepository;

    public RaffleActivityAccountQuotaSupport(IActivityRespository activityRepository, DefaultActivityChainFactory defaultActivityChainFactory) {
        this.activityRepository = activityRepository;
        this.defaultActivityChainFactory = defaultActivityChainFactory;
    }

    public ActivitySkuEntity queryActivitySku(Long sku) {
        return activityRepository.queryActivitySku(sku);
    }

    public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
        return activityRepository.queryRaffleActivityByActivityId(activityId);
    }

    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        return activityRepository.queryRaffleActivityCountByActivityCountId(activityCountId);
    }


}
