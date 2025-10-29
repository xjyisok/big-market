package cn.bugstack.domain.strategy.service.armory.algorithm;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;

import java.math.BigDecimal;
import java.util.List;

public interface IAlgorithm {
    void armoryAlgorithm(String key, List<StrategyAwardEntity> strategyAwardEntities, BigDecimal raterange,BigDecimal minrate);
    Integer dispatchAlgorithm(String key);
}
