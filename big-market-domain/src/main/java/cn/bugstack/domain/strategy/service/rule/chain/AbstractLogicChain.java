package cn.bugstack.domain.strategy.service.rule.chain;

public abstract class AbstractLogicChain implements ILogicChain {
    private ILogicChain next;
    @Override
    public ILogicChain next() {
        return next;
    }

    @Override
    public ILogicChain appendnext(ILogicChain chain) {
        this.next = chain;
        return next;
    }
}
