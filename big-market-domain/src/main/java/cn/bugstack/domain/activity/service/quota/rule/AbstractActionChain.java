package cn.bugstack.domain.activity.service.quota.rule;

public abstract class AbstractActionChain implements IActionChain {
    private IActionChain next;

    @Override
    public IActionChain next() {
        return this.next;
    }

    @Override
    public IActionChain appendNext(IActionChain next) {
        this.next = next;
        return next;
    }
}
