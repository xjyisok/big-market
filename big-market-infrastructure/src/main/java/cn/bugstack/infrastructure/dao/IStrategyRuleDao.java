package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.StrategyAward;
import cn.bugstack.infrastructure.dao.po.StrategyRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IStrategyRuleDao {
    List<StrategyRule> queryStrategyRuleList();

    StrategyRule queryStrategyRule(StrategyRule strategyRuleReq);

    String queryStrategyRuleValue(StrategyRule strategyRule);

    String queryStrategyAwardRuleModels(StrategyAward strategyAward);
}
