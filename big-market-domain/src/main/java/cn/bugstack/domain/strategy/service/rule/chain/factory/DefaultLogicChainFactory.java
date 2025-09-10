package cn.bugstack.domain.strategy.service.rule.chain.factory;

import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
@Component
public class DefaultLogicChainFactory {
    private final Map<String, ILogicChain> logicChainGroup;
    IStrategyRespository strategyRespository;
    public DefaultLogicChainFactory(Map<String, ILogicChain> logicChainGroup, IStrategyRespository strategyRespository) {
        this.logicChainGroup = logicChainGroup;
        this.strategyRespository = strategyRespository;
        System.out.println("=== ILogicChain Beans ===");
        logicChainGroup.forEach((k, v) -> System.out.println("BeanName=" + k + ", BeanClass=" + v.getClass().getName()));
    }
    public ILogicChain openLogicChain(Long strategyId) {
        StrategyEntity strategyentity=strategyRespository.queryStrategyEntityByStrategyId(strategyId);
        String[] ruleModels=strategyentity.ruleModels();
        if(ruleModels==null || ruleModels.length==0){
            return logicChainGroup.get("default");
        }
        System.out.println(ruleModels);
        ILogicChain logicChain=logicChainGroup.get(ruleModels[0]);
        ILogicChain currentchain=logicChain;
        for(int i=1;i<ruleModels.length;i++) {
            ILogicChain nextchain=logicChainGroup.get(ruleModels[i]);
            currentchain=currentchain.appendnext(nextchain);
        }
        currentchain.appendnext(logicChainGroup.get("default"));
        return logicChain;
    }
    @Getter
    @AllArgsConstructor
    public enum LogicModel{
        RULE_DEFAULT("default","默认抽奖"),
        RULE_WEIGHT("rule_weight","权重规则"),
        RULE_BLACKLIST("rule_blacklist","黑名单抽奖");
        private final String ruleModel;
        private final String info;

    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StrategyAwardVO{
        private Integer awardId;
        private String logicModel;
    }
    /**
     * 抽奖计算，责任链抽象方法
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @return 奖品ID
     */


}

