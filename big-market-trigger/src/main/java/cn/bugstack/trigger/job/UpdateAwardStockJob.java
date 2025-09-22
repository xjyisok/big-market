package cn.bugstack.trigger.job;

import cn.bugstack.domain.strategy.model.vo.StrategyAwardStockModelVO;
import cn.bugstack.domain.strategy.service.IRaffleStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Component
public class UpdateAwardStockJob {
    @Resource
    private IRaffleStock raffleStock;
    @Scheduled(cron = "0/5 * * * * ?")
    public void updateAwardStock(){
        try {
            //log.info("定时任务，更新奖品消耗库存【延迟队列获取，降低对数据库的更新频次，不要产生竞争】");
            StrategyAwardStockModelVO strategyAwardStockModelVO = raffleStock.takeQueueValue();
            if(strategyAwardStockModelVO == null){
                log.info("当前无事务");
            }
            else{
            log.info("更新策略 strategyId{},awardId:{}", strategyAwardStockModelVO.getStrategyId(), strategyAwardStockModelVO.getAwardId());
            Long strategyId = strategyAwardStockModelVO.getStrategyId();
            Integer awardId = strategyAwardStockModelVO.getAwardId();
            raffleStock.updateStrategyAwardStock(strategyId, awardId);
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }
}
