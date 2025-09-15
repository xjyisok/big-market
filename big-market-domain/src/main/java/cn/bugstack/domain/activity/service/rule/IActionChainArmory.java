package cn.bugstack.domain.activity.service.rule;

/**
 * 抽奖动作责任链
 */
public interface IActionChainArmory {
    IActionChain next();
    IActionChain appendNext(IActionChain next);
}
