package cn.bugstack.domain.activity.service.quota.policy;

import cn.bugstack.domain.activity.model.aggerate.CreateQuotaOrderAggregate;

public interface ITradePolicy {
    void trade(CreateQuotaOrderAggregate quotaOrderAggregate);
}
