package cn.bugstack.domain.strategy.service.armory;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.armory.algorithm.AbstractAlgorithm;
import cn.bugstack.domain.strategy.service.armory.algorithm.IAlgorithm;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.omg.CORBA.portable.ApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

/*
@description 策略装配库负责初始化策略兵工厂
 */
@Service
@Slf4j
public class StrategyArmoryDispathchImpl extends AbstractStrategyArmoryDispatch {
//    @Autowired
//    private IStrategyRespository strategyRespository;
//    @Override
//    public boolean assembleLotteryStrategy(Long strategyId) {
//        //1.查询策略配置
//        List<StrategyAwardEntity>strategyAwardEntityList=strategyRespository.queryStrategyAwardList(strategyId);
//        for(StrategyAwardEntity strategyAwardEntity:strategyAwardEntityList){
//            System.out.println(JSON.toJSONString(strategyAwardEntity));
//        }
//        for(StrategyAwardEntity strategyAwardEntity:strategyAwardEntityList){
//            Integer awardId=strategyAwardEntity.getAwardId();
//            Integer awardcount=strategyAwardEntity.getAwardCount();
//            cacheStrategyAwardCount(strategyId,awardcount,awardId);
//
//        }
//        assembleLotteryStrategy(String.valueOf(strategyId), strategyAwardEntityList);
//        //2.权重策略配置-适用于rule-weight权重规则配置
//        StrategyEntity strategyEntity=strategyRespository.queryStrategyEntityByStrategyId(strategyId);
//        String ruleweight=strategyEntity.getRuleWeight();
//        if(ruleweight==null){
//            return true;
//        }
//        StrategyRuleEntity strategyRuleEntity=strategyRespository.queryStrategyRule(strategyId,ruleweight);
//        if(strategyRuleEntity==null){
//            throw new AppException(ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getCode(),ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getInfo());
//        }
//        Map<String, List<Integer>> ruleWeightValueMap = strategyRuleEntity.getRuleWeightValues();
//        Set<String> keys = ruleWeightValueMap.keySet();
//        for (String key : keys) {
//            List<Integer> ruleWeightValues = ruleWeightValueMap.get(key);
//            ArrayList<StrategyAwardEntity> strategyAwardEntitiesClone = new ArrayList<>(strategyAwardEntityList);
//            strategyAwardEntitiesClone.removeIf(entity -> !ruleWeightValues.contains(entity.getAwardId()));
//            assembleLotteryStrategy(String.valueOf(strategyId).concat("_").concat(key), strategyAwardEntitiesClone);
//        }
//        return true;
//
//    }
//    private void assembleLotteryStrategy(String key,List<StrategyAwardEntity>strategyAwardEntityList){
//        BigDecimal minrate=strategyAwardEntityList.stream().map(StrategyAwardEntity::getAwardRate).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
//        BigDecimal totalrate=strategyAwardEntityList.stream().map(StrategyAwardEntity::getAwardRate).reduce(BigDecimal.ZERO, BigDecimal::add);
//        BigDecimal range=totalrate.divide(minrate,0,BigDecimal.ROUND_CEILING);
//        List<Integer>strategyAwardSearchRateTable=new ArrayList<>(range.intValue());
//        for(StrategyAwardEntity strategyAwardEntity:strategyAwardEntityList){
//            int awardId=strategyAwardEntity.getAwardId();
//            BigDecimal awardRate=strategyAwardEntity.getAwardRate();
//            for(int j=0;j<awardRate.divide(minrate,0,BigDecimal.ROUND_CEILING).intValue();j++){
//                strategyAwardSearchRateTable.add(awardId);
//            }
//        }
//        Collections.shuffle(strategyAwardSearchRateTable);
//        HashMap<Integer,Integer> strategyAwardRateMap=new HashMap<>(strategyAwardSearchRateTable.size());
//        for(int i=0;i<strategyAwardSearchRateTable.size();i++){
//            strategyAwardRateMap.put(i,strategyAwardSearchRateTable.get(i));
//        }
//        strategyRespository.storeStrategyAwardSearchTable(key,range,strategyAwardRateMap);
//    }
//    @Override
//    public Integer getRandomAwardId(Long strategyId) {
//        return strategyRespository.getStrategyAwardAssemble(String.valueOf(strategyId),new SecureRandom().nextInt(strategyRespository.getRateRange(strategyId)));
//    }
//    @Override
//    public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
//        String key = String.valueOf(strategyId).concat("_").concat(ruleWeightValue);
//        // 分布式部署下，不一定为当前应用做的策略装配。也就是值不一定会保存到本应用，而是分布式应用，所以需要从 Redis 中获取。
//        int rateRange = strategyRespository.getRateRange(key);
//        // 通过生成的随机值，获取概率值奖品查找表的结果
//        return strategyRespository.getStrategyAwardAssemble(key, new SecureRandom().nextInt(rateRange));
//    }
//    private void cacheStrategyAwardCount(Long strategyId,Integer awardCount,Integer awardId){
//        String cachekey= Constants.RedisKey.STRATEGY_AWARD_COUNT_KEY+strategyId+Constants.UNDERLINE+awardId;
//        //System.out.println("<UNK>"+awardCount+"<UNK>");
//        strategyRespository.cacheStrategyAwardCount(cachekey,awardCount);
//    }
//
//    @Override
//    public Boolean substractAwardCount(Long strategyId, Integer awardId,Date endDateTime) {
//        System.out.println("开始扣减");
//        String key=Constants.RedisKey.STRATEGY_AWARD_COUNT_KEY+strategyId+Constants.UNDERLINE+awardId;
//        return strategyRespository.substractAwardCount(key);
//    }

//    @Override
//    public boolean assembleLotteryStrategyByActivityId(Long activityId) {
//        Long strategyId=strategyRespository.queryStrategyIdByActivity(activityId);
//        return assembleLotteryStrategy(strategyId);
//    }
    private final Map<String, IAlgorithm>algorithmMap;
    private final Integer ALGORITHM_THRESHOLD_VALUE=10000;
    public StrategyArmoryDispathchImpl(Map<String, IAlgorithm> algorithmMap) {
        this.algorithmMap = algorithmMap;
    }

    @Override
    protected void armoryAlgorithm(String key, List<StrategyAwardEntity> strategyAwardEntityList) {
        BigDecimal minrate=strategyAwardEntityList.stream().map(StrategyAwardEntity::getAwardRate).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal totalrate=strategyAwardEntityList.stream().map(StrategyAwardEntity::getAwardRate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal range=totalrate.divide(minrate,4,BigDecimal.ROUND_HALF_DOWN);
        //System.out.println(ALGORITHM_THRESHOLD_VALUE);
        if(range.doubleValue()<ALGORITHM_THRESHOLD_VALUE){
            //System.out.println("O1"+range.doubleValue());
            //System.out.println(ALGORITHM_THRESHOLD_VALUE);
            IAlgorithm o1algorithm=algorithmMap.get(AbstractAlgorithm.AlgorithmType.O1.getKey());
            o1algorithm.armoryAlgorithm(key,strategyAwardEntityList,range,minrate);
            strategyRespository.cacheStrategyArmoryAlgorithm(key,AbstractAlgorithm.AlgorithmType.O1.getKey());
        }
        else{
            //System.out.println("OLOGN"+range.doubleValue());
            IAlgorithm ologNalgorithm=algorithmMap.get(AbstractAlgorithm.AlgorithmType.OLogN.getKey());
            ologNalgorithm.armoryAlgorithm(key,strategyAwardEntityList,range,minrate);
            strategyRespository.cacheStrategyArmoryAlgorithm(key,AbstractAlgorithm.AlgorithmType.OLogN.getKey());
        }
    }

    @Override
    protected Integer dispatchAlgorithm(String key) {
        String beanName=strategyRespository.queryStrategyArmoryAlgorithm(key);
        IAlgorithm algorithm=algorithmMap.get(beanName);
        return algorithm.dispatchAlgorithm(key);
    }
}
