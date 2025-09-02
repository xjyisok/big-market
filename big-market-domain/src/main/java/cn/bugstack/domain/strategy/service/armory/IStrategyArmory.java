package cn.bugstack.domain.strategy.service.armory;

public interface IStrategyArmory {
    public void assembleLotteryStrategy(Long strategyId);
    public Integer getRandomAwardId(Long strategyId);
}
