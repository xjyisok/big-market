package cn.bugstack.domain.strategy.service;

import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactoryEntity;
import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.model.vo.StrategyAwardStockModelVO;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.armory.IStrategyDisPatch;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultLogicChainFactory;
import cn.bugstack.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;

@Slf4j
public abstract class AbstractRaffleStrategy implements IRaffleStrategy,IRaffleStock{
    protected IStrategyRespository respository;
    protected IStrategyDisPatch strategyDispatch;
    protected final DefaultLogicChainFactory defaultLogicChainFactory;
    protected final DefaultTreeFactory defaultTreeFactory;
    public AbstractRaffleStrategy(IStrategyRespository respository, IStrategyDisPatch dispatch, DefaultLogicChainFactory logicChainFactory, DefaultTreeFactory treeFactory) {
        this.respository = respository;
        this.strategyDispatch = dispatch;
        this.defaultLogicChainFactory = logicChainFactory;
        this.defaultTreeFactory = treeFactory;
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
        DefaultLogicChainFactory.StrategyAwardVO chainStrategyAward=raffleLogicChain(userId,strategyId);
        log.info("抽奖策略计算-责任链 {} {} {} {}", userId, strategyId, chainStrategyAward.getAwardId(), chainStrategyAward.getLogicModel());
        if (!DefaultLogicChainFactory.LogicModel.RULE_DEFAULT.getRuleModel().equals(chainStrategyAward.getLogicModel())) {
            System.out.println("被默认拦截了");
//            return RaffleAwardEntity.builder()
//                    .awardId(chainStrategyAward.getAwardId())
//                    .build();
            return buildRaffleAwardEntity(strategyId,chainStrategyAward.getAwardId(),null);
        }

        //Integer awardId=logicChainFactory.openLogicChain(strategyId).logic(userId,strategyId);
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
//        StrategyAwardRuleModelVO ruleModels=respository.queryStrategyAwardRuleModels(strategyId,awardId);
//        RuleActionEntity<RuleActionEntity.RaffleInEntity> ruleActionInEntity =
//                this.doCheckRaffleInLogic(RaffleFactoryEntity.builder()
//                        .userId(userId)
//                        .strategyId(strategyId)
//                        .awardId(awardId)
//                        .build(), ruleModels.raffleCenterRuleModelList());
//        if(RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(ruleActionInEntity.getCode())){
//            log.info("中将规则拦截，通过抽奖后规则");
//                return RaffleAwardEntity.builder()
//                        .awardDesc("rule_luck_award走兜底奖励")
//                        .build();
//        }
        DefaultTreeFactory.StrategyAwardVO treestrategyAwardVO=raffleLogicTree(userId,strategyId,chainStrategyAward.getAwardId());
        log.info("抽奖策略计算-规则树 {} {} {} {}", userId, strategyId, treestrategyAwardVO.getAwardId(), treestrategyAwardVO.getAwardRuleValue());
//        return RaffleAwardEntity.builder()
//                .awardId(treestrategyAwardVO.getAwardId())
//                .awardConfig(treestrategyAwardVO.getAwardRuleValue())
//                .build();
        return buildRaffleAwardEntity(strategyId,treestrategyAwardVO.getAwardId(),treestrategyAwardVO.getAwardRuleValue());
    }
    private RaffleAwardEntity buildRaffleAwardEntity(Long StrategyId,Integer AwardId,String awardConfig) {
        StrategyAwardEntity strategyAwardEntity=respository.queryStrategyAwardEntity(StrategyId,AwardId);
        return RaffleAwardEntity.builder()
                .awardId(AwardId)
                .awardConfig(awardConfig)
                .sort(strategyAwardEntity.getSort())
                .awardTitle(strategyAwardEntity.getAwardTitle())
                .build();
    }
    public abstract DefaultLogicChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId);

    /**
     * 抽奖结果过滤，决策树抽象方法
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     * @return 过滤结果【奖品ID，会根据抽奖次数判断、库存判断、兜底兜里返回最终的可获得奖品信息】
     */
    public abstract DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Integer awardId);

    @Override
    public StrategyAwardStockModelVO takeQueueValue() throws InterruptedException {
        return null;
    }

    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {

    }
}