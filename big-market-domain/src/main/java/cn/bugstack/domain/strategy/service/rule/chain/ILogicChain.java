package cn.bugstack.domain.strategy.service.rule.chain;

import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultLogicChainFactory;

public interface ILogicChain extends ILogicChainArmory{
    DefaultLogicChainFactory.StrategyAwardVO logic(String userId, Long strategyId);
//    ILogicChain appendnext(ILogicChain chain);
//    ILogicChain next();
}
