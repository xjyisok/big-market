package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.aggerate.CreateOrderAggregate;
import cn.bugstack.domain.activity.model.entity.*;
import cn.bugstack.domain.activity.respository.IActivityRespository;
import cn.bugstack.domain.activity.service.rule.IActionChain;
import cn.bugstack.domain.activity.service.rule.factory.DefaultActivityChainFactory;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动抽象类，定义标准的流程
 * @create 2024-03-16 08:42
 */
@Slf4j
@Service
public abstract class AbstractRaffleActivity extends RaffleActivitySupport implements IRaffleOrder {


    public AbstractRaffleActivity(IActivityRespository activityRepository, DefaultActivityChainFactory defaultActivityChainFactory) {
        super(activityRepository, defaultActivityChainFactory);
    }

    @Override
    public ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity) {
        // 1. 通过sku查询活动信息
        ActivitySkuEntity activitySkuEntity = queryActivitySku(activityShopCartEntity.getSku());
        // 2. 查询活动信息
        ActivityEntity activityEntity = queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());
        // 3. 查询次数信息（用户在活动上可参与的次数）
        ActivityCountEntity activityCountEntity = queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());

        log.info("查询结果：{} {} {}", JSON.toJSONString(activitySkuEntity), JSON.toJSONString(activityEntity), JSON.toJSONString(activityCountEntity));

        return ActivityOrderEntity.builder().build();
    }

    @Override
    public String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity) {
        //1参数校验
        String userId=skuRechargeEntity.getUserId();
        Long sku=skuRechargeEntity.getSku();
        String outbusinessNo=skuRechargeEntity.getOutBusinessNo();
        if(sku==null|| StringUtils.isEmpty(outbusinessNo)||StringUtils.isEmpty(userId)){
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        //查询基础信息
        //查询sku活动信息
        ActivitySkuEntity activitySkuEntity = queryActivitySku(sku);
        //查询活动信息
        ActivityEntity activityEntity = queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());
        //查询次数信息
        ActivityCountEntity activityCountEntity = queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());
        //活动动作规则校验
        IActionChain iActionChain= defaultActivityChainFactory.openActionChain();
        boolean result=iActionChain.action(activitySkuEntity, activityEntity, activityCountEntity);
        //构建订单聚合对象
        CreateOrderAggregate createOrderAggregate = builderOrderAggerate(skuRechargeEntity,
                activitySkuEntity,activityEntity,activityCountEntity);
        //保存订单
        doSaveOrder(createOrderAggregate);
        return createOrderAggregate.getActivityOrderEntity().getOrderId();
    }

    protected abstract CreateOrderAggregate builderOrderAggerate(SkuRechargeEntity skuRechargeEntity,
                                                      ActivitySkuEntity activitySkuEntity,
                                                      ActivityEntity activityEntity, ActivityCountEntity activityCountEntity);
    protected abstract void doSaveOrder(CreateOrderAggregate createOrderAggregate);
}
