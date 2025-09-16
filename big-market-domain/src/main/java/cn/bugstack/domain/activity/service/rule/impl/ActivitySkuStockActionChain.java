package cn.bugstack.domain.activity.service.rule.impl;

import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.respository.IActivityRespository;
import cn.bugstack.domain.activity.service.armory.IActivityArmory;
import cn.bugstack.domain.activity.service.armory.IActivityDispatch;
import cn.bugstack.domain.activity.service.rule.AbstractActionChain;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("activity_sku_stock_action")
@Slf4j
public class ActivitySkuStockActionChain extends AbstractActionChain {
    @Resource
    IActivityArmory activityArmory;
    @Resource
    IActivityDispatch activityDispatch;
    @Resource
    IActivityRespository activityRespository;
    @Override
    public boolean action(ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        log.info("活动责任链-库存信息【有效期，状态】校验开始sku:{},activityId:{}",activitySkuEntity.getSku(),activitySkuEntity.getActivityId());
        boolean status=activityDispatch.subtarctionActivitySkuStock(activitySkuEntity.getSku(),activityEntity.getEndDateTime());
        if(status){
            log.info("活动责任链-商品库存处理【有效期库存处理成功】sku:{},activityId:{}",activitySkuEntity.getSku(),activityEntity.getActivityId());
            activityRespository.activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO.builder()
                            .sku(activitySkuEntity.getSku())
                            .activityId(activityEntity.getActivityId())
                    .build());
            return true;
        }
        throw new AppException(ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getCode(),ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getInfo());
    }
}
