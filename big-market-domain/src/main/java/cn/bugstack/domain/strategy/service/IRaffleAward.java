package cn.bugstack.domain.strategy.service;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.vo.StrategyAwardStockModelVO;
import org.springframework.boot.autoconfigure.web.WebProperties;

import java.util.List;

public interface IRaffleAward {
    List<StrategyAwardEntity>queryRaffleStrategyAwardList(Long strategyId);
    List<StrategyAwardEntity>queryRaffleStrategyAwardListByActivityId(Long activityId);
    List<StrategyAwardStockModelVO>queryStrategyAwardStockModelList();
}
