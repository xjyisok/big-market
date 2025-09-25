package cn.bugstack.domain.strategy.service.armory;

import java.util.Date;

/*
策略接口的调度
 */
public interface IStrategyDisPatch {
    public Integer getRandomAwardId(Long strategyId);
    public Integer getRandomAwardId(Long strategyId, String ruleWeightValue);
    public Boolean substractAwardCount(Long strategyId, Integer awardId, Date endDateTime);
}
