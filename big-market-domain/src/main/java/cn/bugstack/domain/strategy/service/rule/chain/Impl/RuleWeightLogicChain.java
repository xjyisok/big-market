package cn.bugstack.domain.strategy.service.rule.chain.Impl;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.armory.IStrategyDisPatch;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultLogicChainFactory;
import cn.bugstack.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Component("rule_weight")
public class RuleWeightLogicChain extends AbstractLogicChain {
    public Long userScore = 0L;
    @Resource
    private IStrategyRespository respository;
    @Resource
    private IStrategyDisPatch strategyDisPatch;
    @Override
    public DefaultLogicChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        log.info("抽奖责任链-权重策略开始userId:{},strategyId:{},rulemodel:{}",userId,strategyId,getruleModel() );
        String ruleValue = respository.
                queryStrategyRuleValue(strategyId, getruleModel());
        Map<Long, String> analyticalValueGroup = getAnalyticalValue(ruleValue);
        if (null == analyticalValueGroup || analyticalValueGroup.isEmpty()) {
            return next().logic(userId, strategyId);
        }
        List<Long> analyticalSortedKeys = new ArrayList<>(analyticalValueGroup.keySet());
        Collections.sort(analyticalSortedKeys,Collections.reverseOrder());
        //{4000,5000,6000}
        // 3. 找出最小符合的值，也就是【4500 积分，能找到 4000:102,103,104,105】、【5000 积分，能找到 5000:102,103,104,105,106,107】
        Long nextValue = analyticalSortedKeys.stream()
                .filter(key -> key<=userScore)
                .findFirst()
                .orElse(null);
        if (null != nextValue) {
            log.info("抽奖责任链-权重策略接管userId:{},strategyId:{},rulemodel:{}",userId,strategyId,getruleModel() );
            return DefaultLogicChainFactory.StrategyAwardVO.builder()
                    .awardId(strategyDisPatch.getRandomAwardId(strategyId,analyticalValueGroup.get(nextValue)))
                    .logicModel(getruleModel())
                    .build();
            //return strategyDisPatch.getRandomAwardId(strategyId,analyticalValueGroup.get(nextValue));
        }
        log.info("抽奖责任链-权重策略放行userId:{},strategyId:{},rulemodel:{}",userId,strategyId,getruleModel() );
        return next().logic(userId, strategyId);

    }
    public String getruleModel(){
        return "rule_weight";
    }
    private Map<Long, String> getAnalyticalValue(String ruleValue) {
        String[] ruleValueGroups = ruleValue.split(Constants.SPACE);
        Map<Long, String> ruleValueMap = new HashMap<>();
        for (String ruleValueKey : ruleValueGroups) {
            // 检查输入是否为空
            if (ruleValueKey == null || ruleValueKey.isEmpty()) {
                return ruleValueMap;
            }
            // 分割字符串以获取键和值
            String[] parts = ruleValueKey.split(Constants.COLON);
            if (parts.length != 2) {
                throw new IllegalArgumentException("rule_weight rule_rule invalid input format" + ruleValueKey);
            }
            ruleValueMap.put(Long.parseLong(parts[0]), ruleValueKey);
        }
        return ruleValueMap;
    }
}
