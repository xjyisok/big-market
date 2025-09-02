package cn.bugstack.domain.strategy.service.armory;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

/*
@description 策略装配库负责初始化策略兵工厂
 */
@Service
@Slf4j
public class StrategyArmoryImpl implements IStrategyArmory {
    @Autowired
    private IStrategyRespository strategyRespository;
    @Override
    public void assembleLotteryStrategy(Long strategyId) {
        List<StrategyAwardEntity>strategyAwardEntityList=strategyRespository.queryStrategyAwardList(strategyId);
        BigDecimal minrate=strategyAwardEntityList.stream().map(StrategyAwardEntity::getAwardRate).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal totalrate=strategyAwardEntityList.stream().map(StrategyAwardEntity::getAwardRate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal range=totalrate.divide(minrate,0,BigDecimal.ROUND_CEILING);
        List<Integer>strategyAwardSearchRateTable=new ArrayList<>(range.intValue());
        for(StrategyAwardEntity strategyAwardEntity:strategyAwardEntityList){
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
        strategyRespository.storeStrategyAwardSearchTable(strategyId,range,strategyAwardRateMap);
    }

    @Override
    public Integer getRandomAwardId(Long strategyId) {
        return strategyRespository.getStrategyAwardKey(strategyId,new SecureRandom().nextInt(strategyRespository.getRateRange(strategyId)));
    }
}
