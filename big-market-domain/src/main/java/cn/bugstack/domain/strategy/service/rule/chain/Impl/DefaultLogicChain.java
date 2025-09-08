package cn.bugstack.domain.strategy.service.rule.chain.Impl;

import cn.bugstack.domain.strategy.service.armory.IStrategyDisPatch;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component("default")
public class DefaultLogicChain extends AbstractLogicChain
{   @Resource
    protected IStrategyDisPatch strategyDisPatch;
    @Override
    public Integer logic(String userId, Long strategyId) {
        Integer awardId=strategyDisPatch.getRandomAwardId(strategyId);
        return awardId;
    }
    public String getruleModel(){
        return "default";
    }
}
