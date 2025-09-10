package cn.bugstack.domain.strategy.service.armory;
/*
策略接口的调度
 */
public interface IStrategyDisPatch {
    public Integer getRandomAwardId(Long strategyId);
    public Integer getRandomAwardId(Long strategyId, String ruleWeightValue);
    public Boolean substractAwardCount(Long strategyId,Integer awardId);
}
