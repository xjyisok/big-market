package cn.bugstack.domain.credit.service;

import cn.bugstack.domain.credit.model.entity.TradeEntity;

public interface ICreditAdjustService {
    public String createOrder(TradeEntity trade);
}
