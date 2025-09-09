package cn.bugstack.domain.strategy.respository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.vo.RuleTreeVO;
import cn.bugstack.domain.strategy.model.vo.StrategyAwardRuleModelVO;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

public interface IStrategyRespository {
    public List<StrategyAwardEntity>queryStrategyAwardList(Long strategyId);
    public void storeStrategyAwardSearchTable(String key, BigDecimal rateRange, HashMap<Integer, Integer> strategyAwardRateMap);
    public Integer getRateRange(Long strategyId);
    public Integer getStrategyAwardAssemble(String key,Integer rate);
    public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);
    public StrategyRuleEntity queryStrategyRule(Long strategyId,String ruleweight);
    public int getRateRange(String key);

    String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel);
    String queryStrategyRuleValue(Long strategyId, String ruleModel);
    StrategyAwardRuleModelVO queryStrategyAwardRuleModels(Long strategyId, Integer awardId);

    RuleTreeVO queryRuleTreeVOByTreeId(String treeId);
}
