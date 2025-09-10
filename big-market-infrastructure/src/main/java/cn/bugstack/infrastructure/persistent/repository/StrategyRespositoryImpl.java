package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.vo.*;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.infrastructure.persistent.dao.*;
import cn.bugstack.infrastructure.persistent.po.*;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    IStrategyRuleDao strategyRuleDao;
    @Autowired
    IStrategyDao strategyDao;
    @Autowired
    IRuleTreeDao ruleTreeDao;
    @Autowired
    IRuleTreeNodeLineDao ruleTreeNodeLineDao;
    @Autowired
    IRuleTreeNodeDao ruleTreeNodeDao;

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
    public void storeStrategyAwardSearchTable(String key, BigDecimal rateRange, HashMap<Integer, Integer> strategyAwardRateMap){
        String rangecache=Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+key;
        redisService.setValue(rangecache,rateRange.intValue());
        Map<Integer,Integer> cacheRateTable=redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+key);
        cacheRateTable.putAll(strategyAwardRateMap);
    }

    @Override
    public Integer getRateRange(Long strategyId) {
        return getRateRange(String.valueOf(strategyId));
    }

    @Override
    public int getRateRange(String key) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+key);
    }

    @Override
    public Integer getStrategyAwardAssemble(String key, Integer rate) {
        return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+key,rate);
    }
    @Override
    public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
        String cachekey=Constants.RedisKey.STRATEGY_KEY+strategyId;
        StrategyEntity strategyEntity=redisService.getValue(cachekey);
        if(strategyEntity!=null){
            return strategyEntity;
        }
        Strategy strategy= strategyDao.queryStrategyListById(strategyId);
        strategyEntity=StrategyEntity.builder()
                .strategyDesc(strategy.getStrategyDesc())
                .strategyId(strategy.getStrategyId())
                .ruleModels(strategy.getRuleModels())
                .build();
        redisService.setValue(cachekey,strategyEntity);
        return strategyEntity;
    }

    @Override
    public StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel) {
        System.out.println(strategyId+ruleModel);
        StrategyRule strategyRuleReq = new StrategyRule();
        strategyRuleReq.setStrategyId(strategyId);
        strategyRuleReq.setRuleModel(ruleModel);
        StrategyRule strategyRuleRes = strategyRuleDao.queryStrategyRule(strategyRuleReq);
        if(strategyRuleRes==null){
            return null;
        }
        return StrategyRuleEntity.builder()
                .strategyId(strategyRuleRes.getStrategyId())
                .awardId(strategyRuleRes.getAwardId())
                .ruleType(strategyRuleRes.getRuleType())
                .ruleModel(strategyRuleRes.getRuleModel())
                .ruleValue(strategyRuleRes.getRuleValue())
                .ruleDesc(strategyRuleRes.getRuleDesc())
                .build();
    }

    @Override
    public String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel) {
        StrategyRule strategyRule=new StrategyRule();
        strategyRule.setStrategyId(strategyId);
        strategyRule.setAwardId(awardId);
        strategyRule.setRuleModel(ruleModel);
        return strategyRuleDao.queryStrategyRuleValue(strategyRule);
    }

    @Override
    public StrategyAwardRuleModelVO queryStrategyAwardRuleModels(Long strategyId, Integer awardId) {
        StrategyAward strategyAward=new StrategyAward();
        strategyAward.setStrategyId(strategyId);
        strategyAward.setAwardId(awardId);
        String ruleModels=strategyRuleDao.queryStrategyAwardRuleModels(strategyAward);
        return StrategyAwardRuleModelVO.builder().ruleModels(ruleModels).build();
    }

    @Override
    public String queryStrategyRuleValue(Long strategyId, String ruleModel) {
        StrategyRule strategyRule=new StrategyRule();
        strategyRule.setStrategyId(strategyId);
        strategyRule.setRuleModel(ruleModel);
        return strategyRuleDao.queryStrategyRuleValue(strategyRule);
    }
    @Override
    public RuleTreeVO queryRuleTreeVOByTreeId(String treeId) {
        String cachekey=Constants.RedisKey.RULE_TREE_VO_KEY+treeId;
        RuleTreeVO ruleTreeVO=redisService.getValue(cachekey);
        if(ruleTreeVO!=null){
            return ruleTreeVO;
        }
        RuleTree ruletree=ruleTreeDao.queryRuleTreeByTreeId(treeId);
        List<RuleTreeNode> ruleTreeNodeList=ruleTreeNodeDao.queryRuleTreeNodeListByTreeId(treeId);
        List<RuleTreeNodeLine>ruleTreeNodeLineList=ruleTreeNodeLineDao.queryRuleTreeNodeLineListByTreeId(treeId);
        Map<String, List<RuleTreeNodeLineVO>>ruleTreeNodeLineMap=new HashMap<>();
        for(RuleTreeNodeLine ruleTreeNodeLine:ruleTreeNodeLineList){
            RuleTreeNodeLineVO ruleTreeNodeLineVO=RuleTreeNodeLineVO.builder()
                    .treeId(treeId)
                    .ruleNodeFrom(ruleTreeNodeLine.getRuleNodeFrom())
                    .ruleNodeTo(ruleTreeNodeLine.getRuleNodeTo())
                    .ruleLimitType(RuleLimitTypeVO.valueOf(ruleTreeNodeLine.getRuleLimitType()))
                    .ruleLimitValue(RuleLogicCheckTypeVO.valueOf(ruleTreeNodeLine.getRuleLimitValue()))
                    .build();
            List<RuleTreeNodeLineVO>ruleTreeNodeLineVOList=ruleTreeNodeLineMap.computeIfAbsent(ruleTreeNodeLine.getRuleNodeFrom(), k -> new ArrayList<>());
            ruleTreeNodeLineVOList.add(ruleTreeNodeLineVO);
        }
        Map<String,RuleTreeNodeVO>ruleTreeNodeVOMap=new HashMap<>();
        for(RuleTreeNode ruleTreeNode:ruleTreeNodeList){
            RuleTreeNodeVO ruleTreeNodeVO=RuleTreeNodeVO.builder()
                    .treeId(treeId)
                    .ruleKey(ruleTreeNode.getRuleKey())
                    .ruleDesc(ruleTreeNode.getRuleDesc())
                    .ruleValue(ruleTreeNode.getRuleValue())
                    .treeNodeLineVOList(ruleTreeNodeLineMap.get(ruleTreeNode.getRuleKey()))
                    .build();
            ruleTreeNodeVOMap.put(ruleTreeNode.getRuleKey(),ruleTreeNodeVO);
        }
        RuleTreeVO ruleTreeVONew=RuleTreeVO.builder()
                .treeId(treeId)
                .treeName(ruletree.getTreeName())
                .treeDesc(ruletree.getTreeDesc())
                .treeRootRuleNode(ruletree.getTreeRootRuleKey())
                .treeNodeMap(ruleTreeNodeVOMap)
                .build();
        redisService.setValue(cachekey, ruleTreeVONew);
        return ruleTreeVONew;
    }

    @Override
    public void cacheStrategyAwardCount(String key, Integer awardcount) {
//        Long count=redisService.getAtomicLong(key);
//        if(count==null){
//            redisService.setAtomicLong(key, awardcount);
//        }else{
//            return;
//        }
        if(null!=redisService.getValue(key)){
            return;
        }
        redisService.setAtomicLong(key, awardcount);
    }

    @Override
    public Boolean substractAwardCount(String key) {
        long surplus= redisService.decr(key);
        if(surplus==0){
            redisService.setAtomicLong(key,0);
            return false;
        }
        String lockkey=key+Constants.UNDERLINE+surplus;
        Boolean lock=redisService.setNx(lockkey);
        if(!lock){
            log.info("争锁失败，当前其他线程正在占用");
        }
        return lock;
    }

    @Override
    public void awardStockConsumeSendQueue(StrategyAwardStockModelVO strategyAwardStockModelVO) {
        String cachekey=Constants.RedisKey.STRATEGY_AWARD_COUNT_QUEUE_KEY;
        RBlockingQueue<StrategyAwardStockModelVO>blockingqueue=redisService.getBlockingQueue(cachekey);
        RDelayedQueue<StrategyAwardStockModelVO>delayedQueue=redisService.getDelayedQueue(blockingqueue);
        delayedQueue.offer(strategyAwardStockModelVO,3, TimeUnit.SECONDS);
    }

    @Override
    public StrategyAwardStockModelVO takeQueueValue() {
        String cachekey=Constants.RedisKey.STRATEGY_AWARD_COUNT_QUEUE_KEY;
        RBlockingQueue<StrategyAwardStockModelVO>blockingqueue=redisService.getBlockingQueue(cachekey);
        return blockingqueue.poll();
    }

    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
        StrategyAward strategyAward=new StrategyAward();
        strategyAward.setStrategyId(strategyId);
        strategyAward.setAwardId(awardId);
        strategyAwardDao.updateStrategyAwardStock(strategyAward);
    }
}

