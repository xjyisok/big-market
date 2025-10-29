package cn.bugstack.domain.strategy.service.armory;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

public abstract class AbstractStrategyArmoryDispatch implements IStrategyArmory,IStrategyDisPatch{
    @Resource
    public IStrategyRespository strategyRespository;
    @Override
    public boolean assembleLotteryStrategyByActivityId(Long activityId) {
        Long strategyId=strategyRespository.queryStrategyIdByActivity(activityId);
        return assembleLotteryStrategy(strategyId);
    }
    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        //1.查询策略配置
        List<StrategyAwardEntity> strategyAwardEntityList=strategyRespository.queryStrategyAwardList(strategyId);
        for(StrategyAwardEntity strategyAwardEntity:strategyAwardEntityList){
            System.out.println(JSON.toJSONString(strategyAwardEntity));
        }
        for(StrategyAwardEntity strategyAwardEntity:strategyAwardEntityList){
            Integer awardId=strategyAwardEntity.getAwardId();
            Integer awardcount=strategyAwardEntity.getAwardCount();
            cacheStrategyAwardCount(strategyId,awardcount,awardId);

        }
        armoryAlgorithm(String.valueOf(strategyId), strategyAwardEntityList);
        //2.权重策略配置-适用于rule-weight权重规则配置
        StrategyEntity strategyEntity=strategyRespository.queryStrategyEntityByStrategyId(strategyId);
        String ruleweight=strategyEntity.getRuleWeight();
        if(ruleweight==null){
            return true;
        }
        StrategyRuleEntity strategyRuleEntity=strategyRespository.queryStrategyRule(strategyId,ruleweight);
        if(strategyRuleEntity==null){
            throw new AppException(ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getCode(),ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getInfo());
        }
        Map<String, List<Integer>> ruleWeightValueMap = strategyRuleEntity.getRuleWeightValues();
        Set<String> keys = ruleWeightValueMap.keySet();
        for (String key : keys) {
            System.out.println(key);
            List<Integer> ruleWeightValues = ruleWeightValueMap.get(key);
            ArrayList<StrategyAwardEntity> strategyAwardEntitiesClone = new ArrayList<>(strategyAwardEntityList);
            strategyAwardEntitiesClone.removeIf(entity -> !ruleWeightValues.contains(entity.getAwardId()));
            armoryAlgorithm(String.valueOf(strategyId).concat("_").concat(key), strategyAwardEntitiesClone);
        }
        return true;
    }

    protected abstract void armoryAlgorithm(String s, List<StrategyAwardEntity> strategyAwardEntityList);

    @Override
    public Integer getRandomAwardId(Long strategyId) {
        return dispatchAlgorithm(String.valueOf(strategyId));
        //return strategyRespository.getStrategyAwardAssemble(String.valueOf(strategyId),new SecureRandom().nextInt(strategyRespository.getRateRange(strategyId)));
    }

    protected abstract Integer dispatchAlgorithm(String s);

    @Override
    public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
        String key = String.valueOf(strategyId).concat("_").concat(ruleWeightValue);
        // 分布式部署下，不一定为当前应用做的策略装配。也就是值不一定会保存到本应用，而是分布式应用，所以需要从 Redis 中获取。
//        int rateRange = strategyRespository.getRateRange(key);
//        // 通过生成的随机值，获取概率值奖品查找表的结果
//        return strategyRespository.getStrategyAwardAssemble(key, new SecureRandom().nextInt(rateRange));
        return dispatchAlgorithm(key);
    }
    private void cacheStrategyAwardCount(Long strategyId,Integer awardCount,Integer awardId){
        String cachekey= Constants.RedisKey.STRATEGY_AWARD_COUNT_KEY+strategyId+Constants.UNDERLINE+awardId;
        //System.out.println("<UNK>"+awardCount+"<UNK>");
        strategyRespository.cacheStrategyAwardCount(cachekey,awardCount);
    }

    @Override
    public Boolean substractAwardCount(Long strategyId, Integer awardId, Date endDateTime) {
        System.out.println("开始扣减");
        String key=Constants.RedisKey.STRATEGY_AWARD_COUNT_KEY+strategyId+Constants.UNDERLINE+awardId;
        return strategyRespository.substractAwardCount(key);
    }
}
