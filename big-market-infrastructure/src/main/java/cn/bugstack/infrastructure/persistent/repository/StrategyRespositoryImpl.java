package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.bugstack.infrastructure.persistent.po.StrategyAward;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

/*
@description 策略仓储实现
 */
@Slf4j
@Service
public class StrategyRespositoryImpl implements IStrategyRespository {
    @Autowired
    IRedisService redisService;
    @Autowired
    IStrategyAwardDao strategyAwardDao;
    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        String cache = Constants.RedisKey.STRATEGY_AWARD_KEY+strategyId;
        List<StrategyAwardEntity>strategyAwardEntityList=redisService.getValue(cache);
        if(strategyAwardEntityList!=null&&strategyAwardEntityList.size()>0){
            return strategyAwardEntityList;
        }
        List<StrategyAward>strategyAwards=strategyAwardDao.queryStrategyAwardListById(strategyId);
        List<StrategyAwardEntity>strategyAwardEntities=new ArrayList<>();
        for(StrategyAward strategyAward:strategyAwards){
            StrategyAwardEntity strategyAwardEntity=new StrategyAwardEntity();
            BeanUtils.copyProperties(strategyAward,strategyAwardEntity);
            strategyAwardEntities.add(strategyAwardEntity);
        }
        redisService.setValue(cache,strategyAwardEntities);
        return strategyAwardEntities;
    }


    @Override
    public void storeStrategyAwardSearchTable(Long StrategyId, BigDecimal rateRange, HashMap<Integer, Integer> strategyAwardRateMap){
        String rangecache=Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+StrategyId;
        redisService.setValue(rangecache,rateRange.intValue());
        Map<Integer,Integer> cacheRateTable=redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+StrategyId);
        cacheRateTable.putAll(strategyAwardRateMap);
    }

    @Override
    public Integer getRateRange(Long strategyId) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+strategyId);
    }

    @Override
    public Integer getStrategyAwardKey(Long strategyId, Integer rate) {
        return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+strategyId,rate);
    }
}
