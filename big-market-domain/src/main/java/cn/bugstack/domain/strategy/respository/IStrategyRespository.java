package cn.bugstack.domain.strategy.respository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

public interface IStrategyRespository {
    public List<StrategyAwardEntity>queryStrategyAwardList(Long strategyId);
    public void storeStrategyAwardSearchTable(Long StrategyId, BigDecimal rateRange, HashMap<Integer, Integer> strategyAwardRateMap);
    public Integer getRateRange(Long strategyId);
    public Integer getStrategyAwardKey(Long strategyId,Integer rate);
}
