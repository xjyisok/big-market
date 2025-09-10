package cn.bugstack.domain.strategy.service.raffle;

import cn.bugstack.domain.strategy.model.entity.RaffleFactoryEntity;
import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.entity.RuleMatterEntity;
import cn.bugstack.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.vo.RuleTreeVO;
import cn.bugstack.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.model.vo.StrategyAwardStockModelVO;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.AbstractRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyDisPatch;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultLogicChainFactory;
import cn.bugstack.domain.strategy.service.rule.filter.ILogicFilter;
import cn.bugstack.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultRaffleStrategy extends AbstractRaffleStrategy {
    @Resource
    private DefaultLogicFactory defaultLogicFactory;

    @Override
    public DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Integer awardId) {
        StrategyAwardRuleModelVO ruleModelVO = respository.queryStrategyAwardRuleModels(strategyId,awardId);
        if(ruleModelVO == null){
            return DefaultTreeFactory.StrategyAwardVO.builder().awardId(awardId).build();
        }
        RuleTreeVO ruletreeVO=respository.queryRuleTreeVOByTreeId(ruleModelVO.getRuleModels());
        if(ruletreeVO == null){
            throw new RuntimeException("存在抽奖策略配置的规则模型Key，未在库表");
        }
        IDecisionTreeEngine treeEngine=defaultTreeFactory.openLogicTree(ruletreeVO);
        return treeEngine.process(userId,strategyId,awardId);
    }

    @Override
    public DefaultLogicChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId) {
        ILogicChain logicChain=defaultLogicChainFactory.openLogicChain(strategyId);
        return logicChain.logic(userId, strategyId);
    }

    public DefaultRaffleStrategy(IStrategyRespository repository, IStrategyDisPatch strategyDispatch,
                                 DefaultLogicChainFactory defaultChainFactory, DefaultTreeFactory treeFactory) {
        super(repository, strategyDispatch, defaultChainFactory,treeFactory);
    }

    @Override
    protected RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(RaffleFactoryEntity raffleFactorEntity, String... logics) {
        if (logics == null || 0 == logics.length) return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();


        Map<String, ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> logicFilterGroup=defaultLogicFactory.openLogicFilter();
        String ruleBackList = Arrays.stream(logics)
                .filter(str -> str.contains(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode()))
                //DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode()="rule_blackList"
                .findFirst()
                .orElse(null);
        //ruleBackList="rule_blackList"
        if (StringUtils.isNotBlank(ruleBackList)) {
            ILogicFilter<RuleActionEntity.RaffleBeforeEntity> logicFilter = logicFilterGroup
                    .get(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode());
            //logicFilterGroup形如{
            //    "rule_weight"   -> WeightLogicFilter 实例,
            //    "rule_blacklist" -> BlacklistLogicFilter 实例
            //}
            /*
            public class RuleMatterEntity {
            用户ID
            private String userId;
            策略ID
            private Long strategyId;
            抽奖奖品ID【规则类型为策略，则不需要奖品ID】
            private Integer awardId;
            抽奖规则类型【rule_random - 随机值计算、rule_lock - 抽奖几次后解锁、rule_luck_award - 幸运奖(兜底奖品)】
            private String ruleModel;
        }    */
            RuleMatterEntity ruleMatterEntity = new RuleMatterEntity();
            ruleMatterEntity.setUserId(raffleFactorEntity.getUserId());
            ruleMatterEntity.setAwardId(ruleMatterEntity.getAwardId());
            ruleMatterEntity.setStrategyId(raffleFactorEntity.getStrategyId());
            ruleMatterEntity.setRuleModel(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode());
            RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleActionEntity = logicFilter.filter(ruleMatterEntity);
            /// ___________________________________________________________
            if (!RuleLogicCheckTypeVO.ALLOW.getCode().equals(ruleActionEntity.getCode())) {
                return ruleActionEntity;
            }//这里说明已经是黑名单用户了，直接返回就行
            //后面对积分累计的保底抽奖做操作
        }

        // 顺序过滤剩余规则
        List<String> ruleList = Arrays.stream(logics)
                .filter(s -> !s.equals(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode()))
                .collect(Collectors.toList());

        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleActionEntity = null;
        //ruleList=“rule_weight”
        for (String ruleModel : ruleList) {
            ILogicFilter<RuleActionEntity.RaffleBeforeEntity> logicFilter = logicFilterGroup.get(ruleModel);
            RuleMatterEntity ruleMatterEntity = new RuleMatterEntity();
            ruleMatterEntity.setUserId(raffleFactorEntity.getUserId());
            ruleMatterEntity.setAwardId(ruleMatterEntity.getAwardId());
            ruleMatterEntity.setStrategyId(raffleFactorEntity.getStrategyId());
            ruleMatterEntity.setRuleModel(ruleModel);
            ruleActionEntity = logicFilter.filter(ruleMatterEntity);
            // 非放行结果则顺序过滤
            log.info("抽奖前规则过滤 userId: {} ruleModel: {} code: {} info: {}",
                    raffleFactorEntity.getUserId(), ruleModel, ruleActionEntity.getCode(), ruleActionEntity.getInfo());
            if (!RuleLogicCheckTypeVO.ALLOW.getCode().equals(ruleActionEntity.getCode())) return ruleActionEntity;
        }

        return ruleActionEntity;
    }

    @Override
    protected RuleActionEntity<RuleActionEntity.RaffleInEntity> doCheckRaffleInLogic(RaffleFactoryEntity raffleFactorEntity, String... logics) {
        if(logics==null||logics.length==0){
            return RuleActionEntity.<RuleActionEntity.RaffleInEntity>builder()
                    .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                    .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                    .build();
        }
        Map<String, ILogicFilter<RuleActionEntity.RaffleInEntity>> logicFilterGroup=defaultLogicFactory.openLogicFilter();
        RuleActionEntity<RuleActionEntity.RaffleInEntity> ruleActionEntity=null;
        for(String logic : logics) {
            ILogicFilter<RuleActionEntity.RaffleInEntity> logicFilter = logicFilterGroup
                    .get(logic);
            RuleMatterEntity ruleMatterEntity = new RuleMatterEntity();
            ruleMatterEntity.setUserId(raffleFactorEntity.getUserId());
            ruleMatterEntity.setAwardId(raffleFactorEntity.getAwardId());
            ruleMatterEntity.setStrategyId(raffleFactorEntity.getStrategyId());
            ruleMatterEntity.setRuleModel(logic);
            ruleActionEntity = logicFilter.filter(ruleMatterEntity);
            if (!RuleLogicCheckTypeVO.ALLOW.getCode().equals(ruleActionEntity.getCode())) {
                return ruleActionEntity;
            }
        }
        return ruleActionEntity;
    }

    @Override
    public StrategyAwardStockModelVO takeQueueValue() throws InterruptedException {
        return respository.takeQueueValue();
    }

    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
        respository.updateStrategyAwardStock(strategyId,awardId);
    }
}
