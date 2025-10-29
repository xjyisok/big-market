package cn.bugstack.domain.strategy.service.armory.algorithm.Impl;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.service.armory.algorithm.AbstractAlgorithm;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
@Component("o1")
public class O1Algorithm extends AbstractAlgorithm {
    @Override
    public void armoryAlgorithm(String key, List<StrategyAwardEntity> strategyAwardEntities, BigDecimal raterange,BigDecimal minrate) {
        List<Integer>strategyAwardSearchRateTable=new ArrayList<>(raterange.intValue());
        for(StrategyAwardEntity strategyAwardEntity:strategyAwardEntities){
            int awardId=strategyAwardEntity.getAwardId();
            BigDecimal awardRate=strategyAwardEntity.getAwardRate();
            for(int j=0;j<awardRate.divide(minrate,0,BigDecimal.ROUND_CEILING).intValue();j++){
                strategyAwardSearchRateTable.add(awardId);
            }
        }
        Collections.shuffle(strategyAwardSearchRateTable);
        HashMap<Integer,Integer> strategyAwardRateMap=new HashMap<>(strategyAwardSearchRateTable.size());
        for(int i=0;i<strategyAwardSearchRateTable.size();i++){
            strategyAwardRateMap.put(i,strategyAwardSearchRateTable.get(i));
        }
        strategyRespository.storeStrategyAwardSearchTable(key,raterange,strategyAwardRateMap);
    }

    @Override
    public Integer dispatchAlgorithm(String key) {
        return strategyRespository.getStrategyAwardAssemble(String.valueOf(key),new SecureRandom().nextInt(strategyRespository.getRateRange(key)));
    }
}
