package cn.bugstack.domain.strategy.service.rule.chain.Impl;

import cn.bugstack.domain.strategy.service.armory.IStrategyDisPatch;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultLogicChainFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component("default")
public class DefaultLogicChain extends AbstractLogicChain
{   @Resource
    protected IStrategyDisPatch strategyDisPatch;
    @Override
    public DefaultLogicChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        Integer awardId=strategyDisPatch.getRandomAwardId(strategyId);
        return DefaultLogicChainFactory.StrategyAwardVO.builder()
                .awardId(awardId)
                .logicModel(getruleModel())
                .build();
    }
    public String getruleModel(){
        return "default";
    }
}
