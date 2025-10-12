package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.activity.model.aggerate.CreatePartakeOrderAggregate;
import cn.bugstack.domain.activity.model.aggerate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.entity.*;
import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.model.valobj.ActivityStateVO;
import cn.bugstack.domain.activity.model.valobj.UserRaffleOrderStateVO;
import cn.bugstack.domain.activity.respository.IActivityRespository;
import cn.bugstack.domain.activity.event.ActivitySkuStockZeroMessageEvent;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.infrastructure.persistent.dao.*;
import cn.bugstack.infrastructure.persistent.po.*;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
public class ActivityRespositoryImpl implements IActivityRespository {
    @Resource
    private IRedisService redisService;
    @Resource
    private IRaffleActivityDao raffleActivityDao;
    @Resource
    private IRaffleActivitySkuDao raffleActivitySkuDao;
    @Resource
    private IRaffleActivityCountDao raffleActivityCountDao;
    @Resource
    private IRaffleActivityOrderDao raffleActivityOrderDao;
    @Resource
    private IRaffleActivityAccountDao raffleActivityAccountDao;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private EventPublisher eventPublisher;
    @Resource
    private ActivitySkuStockZeroMessageEvent activitySkuStockZeroMessageEvent;
    @Resource
    IRaffleActivityAccountDayDao raffleActivityAccountDayDao;
    @Resource
    IRaffleActivityAccountMonthDao raffleActivityAccountMonthDao;
    @Resource
    IUserRaffleOrderDao userRaffleOrderDao;
    @Override
    public ActivitySkuEntity queryActivitySku(Long sku) {
        RaffleActivitySku raffleActivitySku = raffleActivitySkuDao.queryRaffleActivitySku(sku);
        return ActivitySkuEntity.builder()
                .sku(raffleActivitySku.getSku())
                .activityId(raffleActivitySku.getActivityId())
                .activityCountId(raffleActivitySku.getActivityCountId())
                .stockCount(raffleActivitySku.getStockCount())
                .stockCountSurplus(raffleActivitySku.getStockCountSurplus())
                .build();

    }

    @Override
    public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
        String cacheKey = Constants.RedisKey.ACTIVITY_KEY + activityId;
        ActivityEntity activityEntity = redisService.getValue(cacheKey);
        if (null != activityEntity) return activityEntity;
        // 从库中获取数据
        RaffleActivity raffleActivity = raffleActivityDao.queryRaffleActivityByActivityId(activityId);
        activityEntity = ActivityEntity.builder()
                .activityId(raffleActivity.getActivityId())
                .activityName(raffleActivity.getActivityName())
                .activityDesc(raffleActivity.getActivityDesc())
                .beginDateTime(raffleActivity.getBeginDateTime())
                .endDateTime(raffleActivity.getEndDateTime())
                .strategyId(raffleActivity.getStrategyId())
                .state(ActivityStateVO.valueOf(raffleActivity.getState()))
                .build();
        redisService.setValue(cacheKey, activityEntity);
        return activityEntity;

    }

    @Override
    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        String cacheKey = Constants.RedisKey.ACTIVITY_COUNT_KEY + activityCountId;
        ActivityCountEntity activityCountEntity = redisService.getValue(cacheKey);
        if (null != activityCountEntity) return activityCountEntity;
        // 从库中获取数据
        RaffleActivityCount raffleActivityCount = raffleActivityCountDao.queryRaffleActivityCountByActivityCountId(activityCountId);
        activityCountEntity = ActivityCountEntity.builder()
                .activityCountId(raffleActivityCount.getActivityCountId())
                .totalCount(raffleActivityCount.getTotalCount())
                .dayCount(raffleActivityCount.getDayCount())
                .monthCount(raffleActivityCount.getMonthCount())
                .build();
        redisService.setValue(cacheKey, activityCountEntity);
        return activityCountEntity;

    }

    public void doSaveOrder(CreateQuotaOrderAggregate createOrderAggregate) {
        RLock lock = redisService.getLock(Constants.RedisKey.ACTIVITY_ACCOUNT_LOCK + createOrderAggregate.getUserId() + Constants.UNDERLINE + createOrderAggregate.getActivityId());
        try {
            lock.lock(3, TimeUnit.SECONDS);
            // 订单对象
            ActivityOrderEntity activityOrderEntity = createOrderAggregate.getActivityOrderEntity();
            RaffleActivityOrder raffleActivityOrder = new RaffleActivityOrder();
            raffleActivityOrder.setUserId(activityOrderEntity.getUserId());
            raffleActivityOrder.setSku(activityOrderEntity.getSku());
            raffleActivityOrder.setActivityId(activityOrderEntity.getActivityId());
            raffleActivityOrder.setActivityName(activityOrderEntity.getActivityName());
            raffleActivityOrder.setStrategyId(activityOrderEntity.getStrategyId());
            raffleActivityOrder.setOrderId(activityOrderEntity.getOrderId());
            raffleActivityOrder.setOrderTime(activityOrderEntity.getOrderTime());
//            raffleActivityOrder.setTotalCount(activityOrderEntity.getTotalCount());
//            raffleActivityOrder.setDayCount(activityOrderEntity.getDayCount());
//            raffleActivityOrder.setMonthCount(activityOrderEntity.getMonthCount());
            raffleActivityOrder.setTotalCount(createOrderAggregate.getTotalCount());
            System.out.println(createOrderAggregate.getTotalCount());
            raffleActivityOrder.setDayCount(createOrderAggregate.getDayCount());
            System.out.println(createOrderAggregate.getDayCount());
            raffleActivityOrder.setMonthCount(createOrderAggregate.getMonthCount());
            System.out.println(createOrderAggregate.getMonthCount());
            raffleActivityOrder.setState(activityOrderEntity.getState().getCode());
            raffleActivityOrder.setOutBusinessNo(activityOrderEntity.getOutBusinessNo());

            // 账户对象
            RaffleActivityAccount raffleActivityAccount = new RaffleActivityAccount();
            raffleActivityAccount.setUserId(createOrderAggregate.getUserId());
            raffleActivityAccount.setActivityId(createOrderAggregate.getActivityId());
            raffleActivityAccount.setTotalCount(createOrderAggregate.getTotalCount());
            raffleActivityAccount.setTotalCountSurplus(createOrderAggregate.getTotalCount());
            raffleActivityAccount.setDayCount(createOrderAggregate.getDayCount());
            raffleActivityAccount.setDayCountSurplus(createOrderAggregate.getDayCount());
            raffleActivityAccount.setMonthCount(createOrderAggregate.getMonthCount());
            raffleActivityAccount.setMonthCountSurplus(createOrderAggregate.getMonthCount());
            // 账户对象 - 月
            RaffleActivityAccountMonth raffleActivityAccountMonth = new RaffleActivityAccountMonth();
            raffleActivityAccountMonth.setUserId(createOrderAggregate.getUserId());
            raffleActivityAccountMonth.setActivityId(createOrderAggregate.getActivityId());
            raffleActivityAccountMonth.setMonth(raffleActivityAccountMonth.currentMonth());
            raffleActivityAccountMonth.setMonthCount(createOrderAggregate.getMonthCount());
            raffleActivityAccountMonth.setMonthCountSurplus(createOrderAggregate.getMonthCount());

            // 账户对象 - 日
            RaffleActivityAccountDay raffleActivityAccountDay = new RaffleActivityAccountDay();
            raffleActivityAccountDay.setUserId(createOrderAggregate.getUserId());
            raffleActivityAccountDay.setActivityId(createOrderAggregate.getActivityId());
            raffleActivityAccountDay.setDay(raffleActivityAccountDay.getCurrentDay());
            raffleActivityAccountDay.setDayCount(createOrderAggregate.getDayCount());
            raffleActivityAccountDay.setDayCountSurplus(createOrderAggregate.getDayCount());

            // 以用户ID作为切分键，通过 doRouter 设定路由【这样就保证了下面的操作，都是同一个链接下，也就保证了事务的特性】
            dbRouter.doRouter(createOrderAggregate.getUserId());
            // 编程式事务
            transactionTemplate.execute(status -> {
                try {
                    // 1. 写入订单
                    raffleActivityOrderDao.insert(raffleActivityOrder);
                    // 2. 更新账户
                    int count = raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
                    // 3. 创建账户 - 更新为0，则账户不存在，创新新账户。
                    if (0 == count) {
                        raffleActivityAccountDao.insert(raffleActivityAccount);
                    }
                    //@description这里为什么不需要在日月账户不存在的时候创建，因为日月账户是和抽奖活动绑定的，每日/月首次抽奖时会在对应日月账户
                    //表上创建账户，账户余额从raffle_activity_account上取，因此在当日或者当月没有抽奖时不需要创建日月账户，日月账户的作用是为了方便维护当日抽奖记录，
                    //未抽奖前只需要将返利打入到raffle_activity_account里面，抽奖时会从raffle_activity_account里取出对应额度创建账户。
                    //至于在测试中，public void test_createOrder() {
                    //        BehaviorEntity behaviorEntity = new BehaviorEntity();
                    //        behaviorEntity.setUserId("xiaofuge");
                    //        behaviorEntity.setBehaviorTypeVO(BehaviorTypeVO.SIGN);
                    //        // 重复的 OutBusinessNo 会报错唯一索引冲突，这也是保证幂等的手段，确保不会多记账
                    //        behaviorEntity.setOutBusinessNo("20240413");
                    //
                    //        List<String> orderIds = behaviorRebateService.createOrder(behaviorEntity);
                    //        log.info("请求参数：{}", JSON.toJSONString(behaviorEntity));
                    //        log.info("测试结果：{}", JSON.toJSONString(orderIds));
                    //    }是发放签到返利，如果当日知识签到但是没有抽奖那么日月账户不会被创建，因为只有在首次抽奖后后续抽奖才会走日月账户
                    //用户-->签到-->update raffle_activity_account-->抽奖->insert raffle_day_account/raffle_month_account-->后续抽奖-->update{raffle_activity_account，raffle_day_account/raffle_month_account}
                    //                          \<------------------------------------------------|
                    raffleActivityAccountDayDao.addDayAccountQuota(raffleActivityAccountDay);
                    raffleActivityAccountMonthDao.addMonthAccountQuota(raffleActivityAccountMonth);
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入订单记录，唯一索引冲突 userId: {} activityId: {} sku: {}", activityOrderEntity.getUserId(), activityOrderEntity.getActivityId(), activityOrderEntity.getSku(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode());
                }
            });
        } finally {
            dbRouter.clear();
            lock.unlock();
        }
    }

    @Override
    public void cacheActivitySkuStockCount(String key, Integer stockCount) {
        if (redisService.isExists(key)) return;
        redisService.setAtomicLong(key, stockCount);

    }

    @Override
    public boolean subtractionSkuStockCount(Long sku, String key, Date endDateTime) {
        long surplus=redisService.decr(key);
        if(surplus==0){
            //库存消耗完了以后发送MQ消息更新数据库库存
            eventPublisher.publish(activitySkuStockZeroMessageEvent.topic(),activitySkuStockZeroMessageEvent.buildEventMessage(sku));
            return false;
        }else if(surplus<0){
            redisService.setAtomicLong(key,0);
            return false;
        }
        //按照decr后的值99，98，97，96和key组成库存锁的key使用
        //加锁为了兜底如果后续有库存恢复手动处理也不会超卖，所有的可用库存key都被加锁了
        //设置加锁时间为活动到期加延迟一天
        String lockKey=key+Constants.UNDERLINE+surplus;
        long expireTime=endDateTime.getTime()-System.currentTimeMillis()+ DateUtils.MILLIS_PER_DAY;
        Boolean lock=redisService.setNx(lockKey,expireTime, TimeUnit.MILLISECONDS);
        if(!lock){
            log.info("活动库存扣减失败{}",lockKey);
        }
        return lock;
    }

    @Override
    public void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO) {
        String cachekey=Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY+Constants.UNDERLINE+activitySkuStockKeyVO.getSku();
        RBlockingQueue<ActivitySkuStockKeyVO>blockingQueue=redisService.getBlockingQueue(cachekey);
        RDelayedQueue<ActivitySkuStockKeyVO>delayedQueue=redisService.getDelayedQueue(blockingQueue);
        delayedQueue.offer(activitySkuStockKeyVO,3,TimeUnit.SECONDS);
    }

    @Override
    public ActivitySkuStockKeyVO takeQueueValue(Long sku) {
        String cachekey=Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY+Constants.UNDERLINE+sku;
        RBlockingQueue<ActivitySkuStockKeyVO>blockingQueue=redisService.getBlockingQueue(cachekey);
        return blockingQueue.poll();
    }

    @Override
    public void clearQueueValue(Long sku) {
        String cachekey=Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY+Constants.UNDERLINE+sku;
        RBlockingQueue<ActivitySkuStockKeyVO>blockingQueue=redisService.getBlockingQueue(cachekey);
        blockingQueue.clear();
    }
    //扫描redis中所有sku队列的sku值
    @Override
    public List<Long> scanAllSkuFromQueue() {
        String pattern = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY + Constants.UNDERLINE + "*";
        Iterable<String> keys = redisService.scanKeys(pattern);

        List<Long> skuList = new ArrayList<>();
        for (String key : keys) {
            // key 格式：ACTIVITY_SKU_COUNT_QUERY_KEY_{sku}
            String[] parts = key.split(Constants.UNDERLINE);
            if (parts.length > 0) {
                try {
                    Long sku = Long.valueOf(parts[parts.length - 1]);
                    skuList.add(sku);
                } catch (NumberFormatException e) {
                    // 忽略解析失败的 key
                }
            }
        }
        return skuList;
    }

    @Override
    public void updateActivitySkuStock(Long sku) {
        raffleActivitySkuDao.updateActivitySkuStock(sku);
    }

    @Override
    public void clearActivitySkuStock(Long sku) {
        raffleActivitySkuDao.clearActivitySkuStock(sku);
    }

    @Override
    public UserRaffleOrderEntity queryNoUsedRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity) {
        UserRaffleOrder userRaffleOrder=new UserRaffleOrder() ;
        userRaffleOrder.setUserId(partakeRaffleActivityEntity.getUserId());
        userRaffleOrder.setActivityId(partakeRaffleActivityEntity.getActivityId());
        UserRaffleOrder userRaffleOrderres=userRaffleOrderDao.queryNoUsedRaffleOrder(userRaffleOrder);
        if(null==userRaffleOrderres){
            return null;
        }
        UserRaffleOrderEntity userRaffleOrderEntity=new UserRaffleOrderEntity();
        userRaffleOrderEntity.setUserId(partakeRaffleActivityEntity.getUserId());
        userRaffleOrderEntity.setActivityId(partakeRaffleActivityEntity.getActivityId());
        userRaffleOrderEntity.setOrderTime(userRaffleOrderres.getOrderTime());
        userRaffleOrderEntity.setActivityName(userRaffleOrderres.getActivityName());
        userRaffleOrderEntity.setStrategyId(userRaffleOrderres.getStrategyId());
        userRaffleOrderEntity.setOrderState(UserRaffleOrderStateVO.valueOf(userRaffleOrderres.getOrderState()));
        userRaffleOrderEntity.setOrderId(userRaffleOrderres.getOrderId());
        return userRaffleOrderEntity;
    }

    @Override
    public ActivityAccountEntity queryActivityAccountByUserId(String userId,Long activityId) {
        RaffleActivityAccount raffleActivityAccount=new RaffleActivityAccount();
        raffleActivityAccount.setUserId(userId);
        raffleActivityAccount.setActivityId(activityId);
        RaffleActivityAccount raffleActivityAccountRes=raffleActivityAccountDao.queryActivityAccountByUserId(raffleActivityAccount);
        if(null==raffleActivityAccountRes){
            return null;
        }
        return ActivityAccountEntity.builder()
                .userId(raffleActivityAccountRes.getUserId())
                .activityId(raffleActivityAccountRes.getActivityId())
                .totalCount(raffleActivityAccountRes.getTotalCount())
                .totalCountSurplus(raffleActivityAccountRes.getTotalCountSurplus())
                .dayCount(raffleActivityAccountRes.getDayCount())
                .dayCountSurplus(raffleActivityAccountRes.getDayCountSurplus())
                .monthCount(raffleActivityAccountRes.getMonthCount())
                .monthCountSurplus(raffleActivityAccountRes.getMonthCountSurplus())
                .build();

    }

    @Override
    public ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, String month) {
        RaffleActivityAccountMonth raffleActivityAccountMonthReq = new RaffleActivityAccountMonth();
        raffleActivityAccountMonthReq.setUserId(userId);
        raffleActivityAccountMonthReq.setActivityId(activityId);
        raffleActivityAccountMonthReq.setMonth(month);
        RaffleActivityAccountMonth raffleActivityAccountMonthRes = raffleActivityAccountMonthDao.queryActivityAccountMonthByUserId(raffleActivityAccountMonthReq);
        if (null == raffleActivityAccountMonthRes) return null;
        // 2. 转换对象
        return ActivityAccountMonthEntity.builder()
                .userId(raffleActivityAccountMonthRes.getUserId())
                .activityId(raffleActivityAccountMonthRes.getActivityId())
                .month(raffleActivityAccountMonthRes.getMonth())
                .monthCount(raffleActivityAccountMonthRes.getMonthCount())
                .monthCountSurplus(raffleActivityAccountMonthRes.getMonthCountSurplus())
                .build();

    }
    @Override
    public ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, String day) {
        RaffleActivityAccountDay raffleActivityAccountDayReq = new RaffleActivityAccountDay();
        raffleActivityAccountDayReq.setUserId(userId);
        raffleActivityAccountDayReq.setActivityId(activityId);
        raffleActivityAccountDayReq.setDay(day);
        RaffleActivityAccountDay raffleActivityAccountDayRes = raffleActivityAccountDayDao.queryActivityAccountDayByUserId(raffleActivityAccountDayReq);
        if (null == raffleActivityAccountDayRes) return null;
        // 2. 转换对象
        return ActivityAccountDayEntity.builder()
                .userId(raffleActivityAccountDayRes.getUserId())
                .activityId(raffleActivityAccountDayRes.getActivityId())
                .day(raffleActivityAccountDayRes.getDay())
                .dayCount(raffleActivityAccountDayRes.getDayCount())
                .dayCountSurplus(raffleActivityAccountDayRes.getDayCountSurplus())
                .build();
    }

    @Override
    public void saveCreatePartakeOrderAggerate(CreatePartakeOrderAggregate createPartakeOrderAggregate) {
        String userId = createPartakeOrderAggregate.getUserId();
        Long activityId = createPartakeOrderAggregate.getActivityId();
        ActivityAccountEntity activityAccountEntity = createPartakeOrderAggregate.getActivityAccountEntity();
        ActivityAccountDayEntity activityAccountDayEntity = createPartakeOrderAggregate.getActivityAccountDayEntity();
        ActivityAccountMonthEntity activityAccountMonthEntity = createPartakeOrderAggregate.getActivityAccountMonthEntity();
        UserRaffleOrderEntity userRaffleOrderEntity = createPartakeOrderAggregate.getUserRaffleOrderEntity();
        dbRouter.doRouter(userId);
        transactionTemplate.execute(status -> {
            try {
                int totalcount = raffleActivityAccountDao.updateActivityAccountSubtractionQuota(RaffleActivityAccount
                        .builder()
                        .userId(userId)
                        .activityId(activityId)
                        .build());
                if (1 != totalcount) {
                    status.setRollbackOnly();
                    log.warn("写入创建参与活动记录，更新总账户额度不足，异常userId:{} activityId:{}", userId, activityId);
                    throw new AppException(ResponseCode.ACTIVITY_QUOTA_ERROR.getCode(), ResponseCode.ACTIVITY_QUOTA_ERROR.getInfo());
                }
                if (createPartakeOrderAggregate.isExistAccountMonth()) {
                    int updateMonthCount = raffleActivityAccountMonthDao.updateActivityAccountMonthSubtractionQuota(
                            RaffleActivityAccountMonth.builder()
                                    .userId(userId)
                                    .activityId(activityId)
                                    .month(activityAccountMonthEntity.getMonth())
                                    .build());
                    if (1 != updateMonthCount) {
                        // 未更新成功则回滚
                        status.setRollbackOnly();
                        log.warn("写入创建参与活动记录，更新月账户额度不足，异常 userId: {} activityId: {} month: {}", userId, activityId, activityAccountMonthEntity.getMonth());
                        throw new AppException(ResponseCode.ACTIVITY_MONTH_QUOTA_ERROR.getCode(), ResponseCode.ACTIVITY_MONTH_QUOTA_ERROR.getInfo());
                    }
                } else {
                    raffleActivityAccountMonthDao.insertActivityAccountMonth(RaffleActivityAccountMonth.builder()
                            .userId(activityAccountMonthEntity.getUserId())
                            .activityId(activityAccountMonthEntity.getActivityId())
                            .month(activityAccountMonthEntity.getMonth())
                            .monthCount(activityAccountMonthEntity.getMonthCount())
                            .monthCountSurplus(activityAccountMonthEntity.getMonthCountSurplus() - 1)
                            .build());
                    // 新创建月账户，则更新总账表中月镜像额度
                    raffleActivityAccountDao.updateActivityAccountMonthSurplusImageQuota(RaffleActivityAccount.builder()
                            .userId(userId)
                            .activityId(activityId)
                            .monthCountSurplus(activityAccountEntity.getMonthCountSurplus())
                            .build());
                }

                // 3. 创建或更新日账户，true - 存在则更新，false - 不存在则插入
                if (createPartakeOrderAggregate.isExistAccountDay()) {
                    int updateDayCount = raffleActivityAccountDayDao.updateActivityAccountDaySubtractionQuota(RaffleActivityAccountDay.builder()
                            .userId(userId)
                            .activityId(activityId)
                            .day(activityAccountDayEntity.getDay())
                            .build());
                    if (1 != updateDayCount) {
                        // 未更新成功则回滚
                        status.setRollbackOnly();
                        log.warn("写入创建参与活动记录，更新日账户额度不足，异常 userId: {} activityId: {} day: {}", userId, activityId, activityAccountDayEntity.getDay());
                        throw new AppException(ResponseCode.ACTIVITY_DAY_QUOTA_ERROR.getCode(), ResponseCode.ACTIVITY_DAY_QUOTA_ERROR.getInfo());
                    }
                } else {
                    raffleActivityAccountDayDao.insertActivityAccountDay(RaffleActivityAccountDay.builder()
                            .userId(activityAccountDayEntity.getUserId())
                            .activityId(activityAccountDayEntity.getActivityId())
                            .day(activityAccountDayEntity.getDay())
                            .dayCount(activityAccountDayEntity.getDayCount())
                            .dayCountSurplus(activityAccountDayEntity.getDayCountSurplus() - 1)
                            .build());
                    // 新创建日账户，则更新总账表中日镜像额度
                    raffleActivityAccountDao.updateActivityAccountDaySurplusImageQuota(RaffleActivityAccount.builder()
                            .userId(userId)
                            .activityId(activityId)
                            .dayCountSurplus(activityAccountEntity.getDayCountSurplus())
                            .build());
                }

                // 4. 写入参与活动订单
                userRaffleOrderDao.insert(UserRaffleOrder.builder()
                        .userId(userRaffleOrderEntity.getUserId())
                        .activityId(userRaffleOrderEntity.getActivityId())
                        .activityName(userRaffleOrderEntity.getActivityName())
                        .strategyId(userRaffleOrderEntity.getStrategyId())
                        .orderId(userRaffleOrderEntity.getOrderId())
                        .orderTime(userRaffleOrderEntity.getOrderTime())
                        .orderState(userRaffleOrderEntity.getOrderState().getCode())
                        .build());


            }catch (DuplicateKeyException e){
                status.setRollbackOnly();
                log.error("写入参与活动记录唯一索引冲突 userId:{} activityId:{}", userId, activityId);
                throw new AppException(ResponseCode.INDEX_DUP.getCode(), ResponseCode.INDEX_DUP.getInfo());
            }
            return 1;
        });
    }

    @Override
    public List<ActivitySkuEntity> queryActivitySkuByActivityId(Long activityId) {
        List<RaffleActivitySku> raffleActivitySkus = raffleActivitySkuDao.queryActivitySkuListByActivityId(activityId);
        List<ActivitySkuEntity> activitySkuEntities = new ArrayList<>(raffleActivitySkus.size());
        for (RaffleActivitySku raffleActivitySku:raffleActivitySkus){
            ActivitySkuEntity activitySkuEntity = new ActivitySkuEntity();
            activitySkuEntity.setSku(raffleActivitySku.getSku());
            activitySkuEntity.setActivityCountId(raffleActivitySku.getActivityCountId());
            activitySkuEntity.setStockCount(raffleActivitySku.getStockCount());
            activitySkuEntity.setStockCountSurplus(raffleActivitySku.getStockCountSurplus());
            activitySkuEntities.add(activitySkuEntity);
        }
        return activitySkuEntities;

    }

    @Override
    public int queryRaffleActivityAccountDayPartakeCount(String userId, Long activityId) {
        RaffleActivityAccountDay raffleActivityAccountDay=new RaffleActivityAccountDay();
        raffleActivityAccountDay.setUserId(userId);
        raffleActivityAccountDay.setActivityId(activityId);
        raffleActivityAccountDay.setDay(raffleActivityAccountDay.getCurrentDay());
        Integer count=raffleActivityAccountDayDao.queryRaffleActivityAccountDayPartakeCount(raffleActivityAccountDay);
        return count!=null?count:0;
    }

    @Override
    public ActivityAccountEntity queryUserActivityAccount(String userId, Long activityId) {
        RaffleActivityAccount raffleActivityAccount=new RaffleActivityAccount();
        raffleActivityAccount.setUserId(userId);
        raffleActivityAccount.setActivityId(activityId);
        RaffleActivityAccount raffleActivityAccountRes=raffleActivityAccountDao.queryActivityAccountByUserId(raffleActivityAccount);
        if (raffleActivityAccountRes == null){
            return ActivityAccountEntity.builder()
                    .userId(userId)
                    .activityId(activityId)
                    .dayCount(0)
                    .dayCountSurplus(0)
                    .monthCount(0)
                    .monthCountSurplus(0)
                    .totalCount(0)
                    .totalCountSurplus(0)
                    .build();
        }
        RaffleActivityAccountMonth raffleActivityAccountMonthres=new RaffleActivityAccountMonth();
        raffleActivityAccountMonthres.setUserId(userId);
        raffleActivityAccountMonthres.setActivityId(activityId);
        raffleActivityAccountMonthres.setMonth(raffleActivityAccountMonthres.getMonth());
        RaffleActivityAccountMonth raffleActivityAccountMonth = raffleActivityAccountMonthDao.queryActivityAccountMonthByUserId(raffleActivityAccountMonthres);

        // 3. 查询日账户额度
        RaffleActivityAccountDay raffleActivityAccountDayres=new RaffleActivityAccountDay();
        raffleActivityAccountDayres.setUserId(userId);
        raffleActivityAccountDayres.setActivityId(activityId);
        raffleActivityAccountDayres.setDay(raffleActivityAccountDayres.getCurrentDay());
        RaffleActivityAccountDay raffleActivityAccountDay = raffleActivityAccountDayDao.queryActivityAccountDayByUserId(raffleActivityAccountDayres);

        // 组装对象
        ActivityAccountEntity activityAccountEntity = new ActivityAccountEntity();
        activityAccountEntity.setUserId(userId);
        activityAccountEntity.setActivityId(activityId);
        activityAccountEntity.setTotalCount(raffleActivityAccountRes.getTotalCount());
        activityAccountEntity.setTotalCountSurplus(raffleActivityAccountRes.getTotalCountSurplus());

        // 如果没有创建日账户，则从总账户中获取日总额度填充。「当新创建日账户时，会获得总账户额度」
        if (null == raffleActivityAccountDay) {
            activityAccountEntity.setDayCount(raffleActivityAccountRes.getDayCount());
            activityAccountEntity.setDayCountSurplus(raffleActivityAccountRes.getDayCount());
        } else {
            activityAccountEntity.setDayCount(raffleActivityAccountDay.getDayCount());
            activityAccountEntity.setDayCountSurplus(raffleActivityAccountDay.getDayCountSurplus());
        }

        // 如果没有创建月账户，则从总账户中获取月总额度填充。「当新创建日账户时，会获得总账户额度」
        if (null == raffleActivityAccountMonth) {
            activityAccountEntity.setMonthCount(raffleActivityAccountRes.getMonthCount());
            activityAccountEntity.setMonthCountSurplus(raffleActivityAccountRes.getMonthCount());
        } else {
            activityAccountEntity.setMonthCount(raffleActivityAccountMonth.getMonthCount());
            activityAccountEntity.setMonthCountSurplus(raffleActivityAccountMonth.getMonthCountSurplus());
        }

        return activityAccountEntity;

    }

    @Override
    public Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId) {
        RaffleActivityAccount raffleActivityAccount=new RaffleActivityAccount();
        raffleActivityAccount.setUserId(userId);
        raffleActivityAccount.setActivityId(activityId);
        RaffleActivityAccount raffleActivityAccountRes=raffleActivityAccountDao.queryActivityAccountByUserId(raffleActivityAccount);
        return raffleActivityAccountRes.getTotalCount()-raffleActivityAccountRes.getTotalCountSurplus();
    }
}
