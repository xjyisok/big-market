package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.activity.model.entity.ActivityAccountEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.vo.*;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultLogicChainFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.bugstack.infrastructure.persistent.dao.*;
import cn.bugstack.infrastructure.persistent.po.*;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static cn.bugstack.types.enums.ResponseCode.UN_ASSEMBLED_STRATEGY_ARMORY;

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
    @Resource
    IRaffleActivityDao raffleactivityDao;
    @Resource
    IRaffleActivityAccountDayDao raffleactivityAccountDayDao;
    @Resource
    IRaffleActivityAccountDao raffleactivityAccountDao;
    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        String cache = Constants.RedisKey.STRATEGY_AWARD_LIST_KEY+strategyId;
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
        String cacheKey = Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + key;
        if (!redisService.isExists(cacheKey)) {
            throw new AppException(UN_ASSEMBLED_STRATEGY_ARMORY.getCode(), cacheKey + Constants.COLON + UN_ASSEMBLED_STRATEGY_ARMORY.getInfo());
        }

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
        return substractAwardCount(key,null);
    }

    @Override
    public Boolean substractAwardCount(String cachekey, Date endDateTime) {
        long surplus= redisService.decr(cachekey);
        if(surplus<0){
            redisService.setAtomicLong(cachekey,0);
            return false;
        }
        String lockkey=cachekey+Constants.UNDERLINE+surplus;
        Boolean lock=false;
        if(null==endDateTime){
            lock=redisService.setNx(lockkey);
        }else{
            long expireMillies=endDateTime.getTime()-System.currentTimeMillis()+TimeUnit.DAYS.toMillis(1);
            lock=redisService.setNx(lockkey,expireMillies,TimeUnit.MILLISECONDS);
        }
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

    @Override
    public StrategyAwardEntity queryStrategyAwardEntity(Long strategyId,Integer awardId) {
        String cachekey=Constants.RedisKey.STRATEGY_AWARD_KEY+strategyId+Constants.UNDERLINE+awardId;
        StrategyAwardEntity strategyAwardEntity=redisService.getValue(cachekey);
        if(strategyAwardEntity!=null){
            return strategyAwardEntity;
        }
        StrategyAward strategyAward=new StrategyAward();
        strategyAward.setStrategyId(strategyId);
        strategyAward.setAwardId(awardId);
        StrategyAward strategyAwardRes=strategyAwardDao.queryStrategyAward(strategyAward);
        strategyAwardEntity = StrategyAwardEntity.builder()
                .strategyId(strategyAwardRes.getStrategyId())
                .awardId(strategyAwardRes.getAwardId())
                .awardTitle(strategyAwardRes.getAwardTitle())
                .awardSubtitle(strategyAwardRes.getAwardSubtitle())
                .awardCount(strategyAwardRes.getAwardCount())
                .awardCountSurplus(strategyAwardRes.getAwardCountSurplus())
                .awardRate(strategyAwardRes.getAwardRate())
                .sort(strategyAwardRes.getSort())
                .ruleModels(strategyAwardRes.getRuleModels())
                .build();

        redisService.setValue(cachekey,strategyAwardEntity);
        return strategyAwardEntity;
    }

    @Override
    public Long queryStrategyIdByActivity(Long activityId) {
        return raffleactivityDao.queryStrategyIdByActivity(activityId);
    }

    @Override
    public Integer queryTodayUserRaffleCount(String userId, Long strategyId) {
        Long activtiyId=raffleactivityDao.queryActivityIdByStrategyId(strategyId);
        RaffleActivityAccountDay raffleActivityAccountDay=new RaffleActivityAccountDay();
        raffleActivityAccountDay.setUserId(userId);
        raffleActivityAccountDay.setActivityId(activtiyId);
        raffleActivityAccountDay.setDay(raffleActivityAccountDay.getCurrentDay());
        RaffleActivityAccountDay raffleActivityAccountDayres=raffleactivityAccountDayDao.queryActivityAccountDayByUserId(raffleActivityAccountDay);
        if(raffleActivityAccountDayres==null){
            return 0;
        }
        return raffleActivityAccountDayres.getDayCount()-raffleActivityAccountDayres.getDayCountSurplus();
    }

    @Override
    public Map<String, Integer> queryAwardRuleLockCount(String[] treeIds) {
        if(treeIds==null||treeIds.length==0){
            return new HashMap<>();
        }
        Map<String, Integer> map=new HashMap<>();
        List<RuleTreeNode>ruleTreeNodeList=ruleTreeNodeDao.queryRuleLocks(treeIds);
        for(RuleTreeNode ruleTreeNode:ruleTreeNodeList){
            String treeId=ruleTreeNode.getTreeId();
            Integer rulevalue=Integer.parseInt(ruleTreeNode.getRuleValue());
            map.put(treeId,rulevalue);
        }
        return map;
    }

    @Override
    public Integer queryActivityAccountTotalUsedCount(String userId, Long strategyId) {
        Long activtiyId=raffleactivityDao.queryActivityIdByStrategyId(strategyId);
        RaffleActivityAccount raffleActivityAccount=
                raffleactivityAccountDao.queryActivityAccountByUserId(RaffleActivityAccount.builder()
                                .activityId(activtiyId)
                        .userId(userId)
                        .build());
        return raffleActivityAccount.getTotalCount()-raffleActivityAccount.getTotalCountSurplus();
    }

    @Override
    public List<RuleWeightVO> queryAwardRuleWeight(Long strategyId) {
        String cacheKey = Constants.RedisKey.STRATEGY_RULE_WEIGHT_KEY + strategyId;
        List<RuleWeightVO> ruleWeightVOS = redisService.getValue(cacheKey);
        if (null != ruleWeightVOS) return ruleWeightVOS;

        ruleWeightVOS = new ArrayList<>();
        // 1. 查询权重规则配置
        StrategyRule strategyRuleReq = new StrategyRule();
        strategyRuleReq.setStrategyId(strategyId);
        strategyRuleReq.setRuleModel(DefaultLogicChainFactory.LogicModel.RULE_WEIGHT.getRuleModel());
        String ruleValue = strategyRuleDao.queryStrategyRuleValue(strategyRuleReq);
        // 2. 借助实体对象转换规则
        StrategyRuleEntity strategyRuleEntity = new StrategyRuleEntity();
        strategyRuleEntity.setRuleModel(DefaultLogicChainFactory.LogicModel.RULE_WEIGHT.getRuleModel());
        strategyRuleEntity.setRuleValue(ruleValue);
        Map<String, List<Integer>> ruleWeightValues = strategyRuleEntity.getRuleWeightValues();
        // 3. 遍历规则组装奖品配置
        Set<String> ruleWeightKeys = ruleWeightValues.keySet();
        for (String ruleWeightKey : ruleWeightKeys) {
            List<Integer> awardIds = ruleWeightValues.get(ruleWeightKey);
            List<RuleWeightVO.Award> awardList = new ArrayList<>();
            // 也可以修改为一次从数据库查询
            for (Integer awardId : awardIds) {
                StrategyAward strategyAwardReq = new StrategyAward();
                strategyAwardReq.setStrategyId(strategyId);
                strategyAwardReq.setAwardId(awardId);
                StrategyAward strategyAward = strategyAwardDao.queryStrategyAward(strategyAwardReq);
                awardList.add(RuleWeightVO.Award.builder()
                        .awardId(strategyAward.getAwardId())
                        .awardTitle(strategyAward.getAwardTitle())
                        .build());
            }

            ruleWeightVOS.add(RuleWeightVO.builder()
                    .ruleValue(ruleValue)
                    .weight(Integer.valueOf(ruleWeightKey.split(Constants.COLON)[0]))
                    .awardIds(awardIds)
                    .awardList(awardList)
                    .build());
        }

        // 设置缓存 - 实际场景中，这类数据，可以在活动下架的时候统一清空缓存。
        redisService.setValue(cacheKey, ruleWeightVOS);

        return ruleWeightVOS;

    }
}

