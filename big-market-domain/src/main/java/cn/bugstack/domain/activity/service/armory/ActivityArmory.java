package cn.bugstack.domain.activity.service.armory;

import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.respository.IActivityRespository;
import cn.bugstack.types.common.Constants;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Date;
@Component
public class ActivityArmory implements IActivityArmory, IActivityDispatch {
    @Resource
    IActivityRespository activityRespository;
    @Override
    public boolean assembleActivitySku(Long sku) {
        ActivitySkuEntity activitySkuEntity =activityRespository.queryActivitySku(sku);
        cacheActivitySkuStockCount(sku,activitySkuEntity.getStockCount());
        activityRespository.queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());
        activityRespository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());
        return false;
    }
    private void cacheActivitySkuStockCount(Long sku, Integer stockCount) {
        String key= Constants.RedisKey.ACTIVITY_SKU_STOCK_COUNT_KEY+sku;
        activityRespository.cacheActivitySkuStockCount(key, stockCount);
    }

    @Override
    public boolean subtarctionActivitySkuStock(Long sku, Date endDateTime) {
        String key= Constants.RedisKey.ACTIVITY_SKU_STOCK_COUNT_KEY+sku;
        return activityRespository.subtractionSkuStockCount(sku,key,endDateTime);
    }
}
