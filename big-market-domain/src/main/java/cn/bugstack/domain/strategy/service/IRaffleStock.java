package cn.bugstack.domain.strategy.service;

import cn.bugstack.domain.strategy.model.vo.StrategyAwardStockModelVO;

public interface IRaffleStock {
    StrategyAwardStockModelVO takeQueueValue() throws InterruptedException;
    StrategyAwardStockModelVO takeQueueValue(Long strategyId,Integer awardId) throws InterruptedException;
    void updateStrategyAwardStock(Long strategyId,Integer awardId);
}
