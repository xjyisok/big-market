package cn.bugstack.domain.strategy.service.rule.chain;

public interface ILogicChainArmory {
    ILogicChain appendnext(ILogicChain chain);
    ILogicChain next();
}
