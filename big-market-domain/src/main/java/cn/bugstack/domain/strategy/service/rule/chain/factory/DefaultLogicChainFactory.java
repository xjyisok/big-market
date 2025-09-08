package cn.bugstack.domain.strategy.service.rule.chain.factory;

import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
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
            currentchain=logicChain.appendnext(nextchain);
        }
        currentchain.appendnext(logicChainGroup.get("default"));
        return logicChain;
    }

}
