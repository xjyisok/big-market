package cn.bugstack.domain.strategy.service;

import cn.bugstack.domain.strategy.model.vo.RuleWeightVO;

import java.util.List;
import java.util.Map;

public interface IRaffleRule {
    public Map<String,Integer>queryAwardRuleLockCount(String[] treeIds);

    List<RuleWeightVO> queryAwardRuleWeightByActivityId(Long activityId);

    List<RuleWeightVO> queryAwardRuleWeight(Long ruleId);
}
