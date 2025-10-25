package cn.bugstack.trigger.job;

import cn.bugstack.domain.strategy.model.vo.StrategyAwardStockModelVO;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import cn.bugstack.domain.strategy.service.IRaffleAward;
import cn.bugstack.domain.strategy.service.IRaffleStock;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class UpdateAwardStockJob {
    @Resource
    private IRaffleStock raffleStock;
    @Resource
    private RedissonClient redisson;
    @Resource
    private IRaffleAward raffleAward;
    @Resource
    ThreadPoolExecutor executor;
    //@Scheduled(cron = "0/5 * * * * ?")
    @XxlJob("updateAwardStockJob")
    public void updateAwardStock(){
        RLock lock = redisson.getLock("big-market-updateAwardStockJob");
        boolean isLock = false;
        try {
            isLock=lock.tryLock(3,0, TimeUnit.SECONDS);
            if(!isLock){
                return;
            }
            List<StrategyAwardStockModelVO>strategyAwardStockModelVOList=raffleAward.queryStrategyAwardStockModelList();
            if(null==strategyAwardStockModelVOList){
                return;
            }
            //log.info("定时任务，更新奖品消耗库存【延迟队列获取，降低对数据库的更新频次，不要产生竞争】");
            for(StrategyAwardStockModelVO strategyAwardStockModelVO:strategyAwardStockModelVOList){
                executor.execute(() -> {
                    try {
                        StrategyAwardStockModelVO queuestrategyAwardStockModelVO = raffleStock.
                                takeQueueValue(strategyAwardStockModelVO.getStrategyId(),strategyAwardStockModelVO.getAwardId());
                        if(null==queuestrategyAwardStockModelVO){
                            log.info("当前无事务");
                        }else{
                            log.info("更新策略 strategyId{},awardId:{}", strategyAwardStockModelVO.getStrategyId(), strategyAwardStockModelVO.getAwardId());
                            Long strategyId = strategyAwardStockModelVO.getStrategyId();
                            Integer awardId = strategyAwardStockModelVO.getAwardId();
                            raffleStock.updateStrategyAwardStock(strategyId, awardId);
                        }
                    }
                    catch (InterruptedException e) {
                        log.error("定时任务，更新奖品消耗库存失败 strategyId:{} awardId:{}", strategyAwardStockModelVO.getStrategyId(), strategyAwardStockModelVO.getAwardId());
                    }
                });
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }finally {
            lock.unlock();
        }
    }
}
