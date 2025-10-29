package cn.bugstack.domain.strategy.service.armory.algorithm.Impl;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.service.armory.algorithm.AbstractAlgorithm;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Component("oLogN")
public class OLogNAlgorithm extends AbstractAlgorithm {
    @Override
    public void armoryAlgorithm(String key, List<StrategyAwardEntity> strategyAwardEntities, BigDecimal raterange,BigDecimal minrate) {
        int from=0;
        int to=0;
        //System.out.println(raterange.doubleValue());
        Map<Map<Integer,Integer>,Integer> map = new HashMap<>();
        for(StrategyAwardEntity strategyAwardEntity : strategyAwardEntities){
            Integer awardId = strategyAwardEntity.getAwardId();
            BigDecimal awardrate= strategyAwardEntity.getAwardRate();
            to+=awardrate.divide(minrate,1, RoundingMode.HALF_UP).intValue();
            Map<Integer,Integer>ft=new HashMap<>();
            ft.put(from,to);
            map.put(ft,awardId);
            //System.out.println(from+":"+to);
            from=to+1;
        }
        //System.out.println(map);
        strategyRespository.storeStrategyAwardSearchTable(key,raterange,map);
    }

    @Override
    public Integer dispatchAlgorithm(String key) {
        int rateRange= new SecureRandom().nextInt(strategyRespository.getRateRange(key));
        Map<Map<String,Integer>,Integer> map = strategyRespository.getMap(key);
//        for (Map.Entry<Map<String,Integer>, Integer> entry : map.entrySet()) {
//            System.out.println("key=" + entry.getKey() + ", value=" + entry.getValue());
//        }
        return foreach(rateRange,map);

    }
    private  Integer foreach(int randRange,Map<Map<String,Integer>,Integer> table){
        Integer awardId=null;
        for(Map.Entry<Map<String,Integer>,Integer> entry:table.entrySet()){
            Map<String,Integer> rangeMap=entry.getKey();
            for(Map.Entry<String,Integer> range:rangeMap.entrySet()){
                int from=Integer.parseInt(range.getKey());
                int to=range.getValue();
                if(from<=randRange && to>=randRange){
                    awardId=entry.getValue();
                    break;
                }
            }
            if(awardId!=null){
                break;
            }
        }
        return awardId;
    }
}
