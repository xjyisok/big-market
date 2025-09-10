package cn.bugstack.domain.strategy.service.rule.tree.impl;

import cn.bugstack.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.service.rule.tree.ILogicTreeNode;

import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
/**
 * @author Fuzhengwei bugstack.cn @xjy
 * @description 兜底奖励节点
 * @create 2024-01-27 11:23
 */

@Slf4j
@Component("rule_luck_award")
public class RuleLuckAwardLogicTreeNode implements ILogicTreeNode {
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId,String rulevalue) {
        log.info("规则过滤-保底奖励userId:{},strategyId:{},awardId:{}", userId, strategyId, awardId);
        String[] split = rulevalue.split(":");
        if(split.length == 0){
            log.error("规则过滤-保底奖励配置异常userId:{},strategyId:{},awardId:{}\", userId, strategyId, awardId");
        }
        Integer AwardId=Integer.parseInt(split[0]);
        String RuleValue=split.length>1?split[1]:"";
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                .strategyAwardData(DefaultTreeFactory.StrategyAwardVO.builder()
                        .awardId(AwardId)
                        .awardRuleValue(RuleValue)
                        .build())
                .build();

    }
}
