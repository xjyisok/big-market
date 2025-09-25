package cn.bugstack.domain.strategy.respository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.vo.RuleTreeVO;
import cn.bugstack.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.model.vo.StrategyAwardStockModelVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void cacheStrategyAwardCount(String cachekey, Integer awardcount);

    Boolean substractAwardCount(String key);
    Boolean substractAwardCount(String cachekey, Date endDateTime);
    void awardStockConsumeSendQueue(StrategyAwardStockModelVO strategyAwardStockModelVO);

    StrategyAwardStockModelVO takeQueueValue();

    void updateStrategyAwardStock(Long strategyId, Integer awardId);

    StrategyAwardEntity queryStrategyAwardEntity(Long strategyId,Integer awardId);

    Long queryStrategyIdByActivity(Long activityId);

    Integer queryTodayUserRaffleCount(String userId, Long strategyId);

    Map<String, Integer> queryAwardRuleLockCount(String[] treeIds);
}
