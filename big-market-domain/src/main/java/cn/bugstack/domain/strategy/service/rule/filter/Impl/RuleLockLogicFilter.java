package cn.bugstack.domain.strategy.service.rule.filter.Impl;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.entity.RuleMatterEntity;
import cn.bugstack.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.annotation.LogicStrategy;
import cn.bugstack.domain.strategy.service.rule.filter.ILogicFilter;
import cn.bugstack.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_LOCK)
public class RuleLockLogicFilter implements ILogicFilter<RuleActionEntity.RaffleInEntity> {
    @Resource
    private IStrategyRespository respository;
    private Long userrafflecount=0L;
    @Override
    public RuleActionEntity<RuleActionEntity.RaffleInEntity> filter(RuleMatterEntity ruleMatter) {
        String rule_value=respository.queryStrategyRuleValue(ruleMatter.getStrategyId(),ruleMatter.getAwardId(),ruleMatter.getRuleModel());
        Long raffle_count=Long.parseLong(rule_value);
        if(userrafflecount>=raffle_count){
            return RuleActionEntity.<RuleActionEntity.RaffleInEntity>builder()
                    .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                    .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                    .build();
        }
        return RuleActionEntity.<RuleActionEntity.RaffleInEntity>builder()
                .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                .ruleModel(ruleMatter.getRuleModel())
                .build();
    }
}
