package cn.bugstack.domain.strategy.service;

import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactoryEntity;
import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.armory.IStrategyDisPatch;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultLogicChainFactory;
import cn.bugstack.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;

@Slf4j
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {
    protected IStrategyRespository respository;
    protected IStrategyDisPatch strategyDispatch;
    private final DefaultLogicChainFactory logicChainFactory;
    public AbstractRaffleStrategy(IStrategyRespository respository, IStrategyDisPatch dispatch, DefaultLogicChainFactory logicChainFactory) {
        this.respository = respository;
        this.strategyDispatch = dispatch;
        this.logicChainFactory = logicChainFactory;
    }
    protected abstract RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(RaffleFactoryEntity raffleFactorEntity, String... logics);
    protected abstract RuleActionEntity<RuleActionEntity.RaffleInEntity> doCheckRaffleInLogic(RaffleFactoryEntity raffleFactorEntity, String... logics);
    @Override
    public RaffleAwardEntity performRaffle(RaffleFactoryEntity rafflefactoryentity) {
        String userId=rafflefactoryentity.getUserId();
        Long strategyId=rafflefactoryentity.getStrategyId();
        if(StringUtils.isBlank(userId) || null==strategyId){
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        Integer awardId=logicChainFactory.openLogicChain(strategyId).logic(userId,strategyId);
//        StrategyEntity strategy=respository.queryStrategyEntityByStrategyId(strategyId);
//        //1,100001,抽奖策略,"rule_weight,rule_blacklist",2023-12-09 09:37:19,2023-12-09 18:06:34
//        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleActionEntity =
//                this.doCheckRaffleBeforeLogic(RaffleFactoryEntity.builder().userId(userId).strategyId(strategyId).build(), strategy.ruleModels());
//        //strategy.ruleModels()="rule_weight,rule_blacklist"
//        //------------------------------------------------------
//        if (RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(ruleActionEntity.getCode())) {
//            if (DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode().equals(ruleActionEntity.getRuleModel())) {
//                // 黑名单返回固定的奖品ID
//                return RaffleAwardEntity.builder()
//                        .awardId(ruleActionEntity.getData().getAwardId())
//                        .build();
//            } else if (DefaultLogicFactory.LogicModel.RULE_WIGHT.getCode().equals(ruleActionEntity.getRuleModel())) {
//                // 权重根据返回的信息进行抽奖
//                RuleActionEntity.RaffleBeforeEntity raffleBeforeEntity = ruleActionEntity.getData();
//                String ruleWeightValueKey = raffleBeforeEntity.getRuleWeightValueKey();
//                Integer awardId = strategyDispatch.getRandomAwardId(strategyId, ruleWeightValueKey);
//                return RaffleAwardEntity.builder()
//                        .awardId(awardId)
//                        .build();
//            }
//        }

        // 4. 默认抽奖流程
        //Integer awardId = strategyDispatch.getRandomAwardId(strategyId);
        StrategyAwardRuleModelVO ruleModels=respository.queryStrategyAwardRuleModels(strategyId,awardId);
        RuleActionEntity<RuleActionEntity.RaffleInEntity> ruleActionInEntity =
                this.doCheckRaffleInLogic(RaffleFactoryEntity.builder()
                        .userId(userId)
                        .strategyId(strategyId)
                        .awardId(awardId)
                        .build(), ruleModels.raffleCenterRuleModelList());
        if(RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(ruleActionInEntity.getCode())){
            log.info("中将规则拦截，通过抽奖后规则");
                return RaffleAwardEntity.builder()
                        .awardDesc("rule_luck_award走兜底奖励")
                        .build();
        }
        return RaffleAwardEntity.builder()
                .awardId(awardId)
                .build();
    }

}