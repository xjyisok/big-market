package cn.bugstack.domain.strategy.service.rule.tree.impl;

import cn.bugstack.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.vo.StrategyAwardStockModelVO;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.armory.IStrategyDisPatch;
import cn.bugstack.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TransferQueue;
/**
 * @author Fuzhengwei bugstack.cn @xjy
 * @description 库存扣减节点
 * @create 2024-01-27 11:25
 */

@Slf4j
@Component("rule_stock")
public class RuleStockLogicTreeNode implements ILogicTreeNode {
    @Resource
    IStrategyDisPatch strategyDisPatch;
    @Resource
    IStrategyRespository strategyRespository;
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId, String rulevalue, Date endDateTime) {
        log.info("规则过滤-库存扣减userId:{},strategyId:{},awardId:{}", userId, strategyId, awardId);
        Boolean issubstratctsucceed=strategyDisPatch.substractAwardCount(strategyId, awardId,endDateTime);
        System.out.println(issubstratctsucceed);
        if(issubstratctsucceed){
            strategyRespository.awardStockConsumeSendQueue(StrategyAwardStockModelVO.builder()
                            .awardId(awardId)
                            .strategyId(strategyId)
                    .build());
            return  DefaultTreeFactory.TreeActionEntity.builder()
                    .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                    .strategyAwardData(DefaultTreeFactory.StrategyAwardVO
                    .builder()
                            .awardId(awardId)
                            .awardRuleValue("")
                            .build())
                    .build();
        }
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
                .build();

    }
}
