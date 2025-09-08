package cn.bugstack.domain.strategy.service.rule.chain;

public interface ILogicChain extends ILogicChainArmory{
    Integer logic(String userId,Long strategyId);
//    ILogicChain appendnext(ILogicChain chain);
//    ILogicChain next();
}
