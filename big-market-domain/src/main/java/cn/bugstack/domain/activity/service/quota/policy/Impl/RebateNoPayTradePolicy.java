package cn.bugstack.domain.activity.service.quota.policy.Impl;

import cn.bugstack.domain.activity.model.aggerate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.valobj.OrderStateVO;
import cn.bugstack.domain.activity.respository.IActivityRespository;
import cn.bugstack.domain.activity.service.quota.policy.ITradePolicy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Service("rebate_no_pay_trade")
public class RebateNoPayTradePolicy implements ITradePolicy {
    @Resource
    private  IActivityRespository activityRespository;
    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        createQuotaOrderAggregate.setOrderState(OrderStateVO.complete);
        createQuotaOrderAggregate.getActivityOrderEntity().setPayAmount(BigDecimal.ZERO);
        activityRespository.doSaveOrder(createQuotaOrderAggregate);
    }
}
