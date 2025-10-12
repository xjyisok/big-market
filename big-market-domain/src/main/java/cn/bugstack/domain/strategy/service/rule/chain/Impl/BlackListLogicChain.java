package cn.bugstack.domain.strategy.service.rule.chain.Impl;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultLogicChainFactory;
import cn.bugstack.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component("rule_blacklist")
public class BlackListLogicChain extends AbstractLogicChain {
    @Resource
    private IStrategyRespository respository;
    @Override
    public DefaultLogicChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        log.info("抽奖责任链-黑名单开始userId:{},strategyId:{},rulemodel:{}",userId,strategyId,getruleModel() );
        String ruleValue=respository.queryStrategyRuleValue(strategyId,getruleModel());
        String[] splitRuleValue = ruleValue.split(Constants.COLON);
        //splitRuleValue=["100","user000,user001,user002"]
        Integer awardId = Integer.parseInt(splitRuleValue[0]);
        //awardId=100;
        // 过滤其他规则
        String[] userBlackIds = splitRuleValue[1].split(Constants.SPLIT);
        for (String userBlackId : userBlackIds) {
            if (userId.equals(userBlackId)) {
                log.info("抽奖责任链-黑名单接管userId:{},strategyId:{},rulemodel:{}",userId,strategyId,getruleModel() );
                return DefaultLogicChainFactory.StrategyAwardVO.builder()
                        .awardId(awardId)
                        .logicModel(getruleModel())
                        //TODO数据库读取
                        .awardRuleValue("0.01,1")
                        .build();
            }
        }
        log.info("抽奖责任链-黑名单放行userId:{},strategyId:{},rulemodel:{}",userId,strategyId,getruleModel() );
        return next().logic(userId, strategyId);
    }
    public String getruleModel(){
        return "rule_blacklist";
    }
}
