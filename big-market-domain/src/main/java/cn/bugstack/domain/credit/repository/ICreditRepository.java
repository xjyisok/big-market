package cn.bugstack.domain.credit.repository;

import cn.bugstack.domain.credit.model.aggergate.TradeAggregate;

public interface ICreditRepository {
    void saveUserCreditTradeOrder(TradeAggregate tradeAggregate);
}
