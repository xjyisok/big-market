package cn.bugstack.domain.rebate.service;

import cn.bugstack.domain.rebate.model.entity.BehaviorEntity;
import cn.bugstack.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.bugstack.domain.rebate.model.valobj.DailyBehaviorRebateVO;

import java.util.List;

public interface IBehaviorRebateService {
    List<String> createOrder(BehaviorEntity behaviorEntity);
    List<BehaviorRebateOrderEntity>queryRebateOrderByOutBusinessId(String userId,String outBusinessId);

}
