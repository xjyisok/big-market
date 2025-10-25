package cn.bugstack.trigger.job;

import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.respository.IActivityRespository;
import cn.bugstack.domain.activity.service.IRaffleActivitySkuStockService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 更新活动sku库存任务
 * @create 2024-03-30 09:52
 */
@Slf4j
@Component()
public class UpdateActivitySkuStockJob {

    @Resource
    private IRaffleActivitySkuStockService skuStock;
    @Resource
    private IActivityRespository activityRespository;
    private ExecutorService executorService;
    @Resource
    private RedissonClient redisson;
    @PostConstruct
    public void init() {
        // 核心线程数（最小线程数）
        int corePoolSize = 5;
        // 最大线程数（并发峰值）
        int maxPoolSize = 20;
        // 空闲线程存活时间（超时回收）
        long keepAliveTime = 60L;
        // 任务队列容量
        int queueCapacity = 100;

        this.executorService = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：主线程执行
        );
    }
    //@Scheduled(cron = "0/5 * * * * ?")
    @XxlJob("UpdateActivitySkuStockJob")
    public void exec() {
        RLock lock = redisson.getLock("big-market-UpdateActivitySkuStockJob");
        boolean isLock = false;
        try {
            isLock= lock.tryLock(3,0, TimeUnit.SECONDS);
            if (!isLock) {
                return;
            }
            List<Long>skuValueOfQueue=activityRespository.scanAllSkuFromQueue();
            //log.info("定时任务，更新活动sku库存【延迟队列获取，降低对数据库的更新频次，不要产生竞争】");
            for(Long skuId:skuValueOfQueue) {
                executorService.submit(()->{
                    try {
                        ActivitySkuStockKeyVO activitySkuStockKeyVO = skuStock.takeQueueValue(skuId);
                        if (null == activitySkuStockKeyVO) return;
                        log.info("定时任务，更新活动sku库存 sku:{} activityId:{}", activitySkuStockKeyVO.getSku(), activitySkuStockKeyVO.getActivityId());
                        skuStock.updateActivitySkuStock(activitySkuStockKeyVO.getSku());
                    }catch (Exception e) {
                        log.error("定时任务，更新活动sku库存失败 skuId:{}", skuId, e);
                    }
                });
//                ActivitySkuStockKeyVO activitySkuStockKeyVO = skuStock.takeQueueValue(skuId);
//                if (null == activitySkuStockKeyVO) return;
//                log.info("定时任务，更新活动sku库存 sku:{} activityId:{}", activitySkuStockKeyVO.getSku(), activitySkuStockKeyVO.getActivityId());
//                skuStock.updateActivitySkuStock(activitySkuStockKeyVO.getSku());
            }
        } catch (Exception e) {
            log.error("定时任务，更新活动sku库存失败", e);
        }
        finally {
            lock.unlock();
        }
    }

}
