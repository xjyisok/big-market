package cn.bugstack.domain.strategy.service;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import org.springframework.boot.autoconfigure.web.WebProperties;

import java.util.List;

public interface IRaffleAward {
    List<StrategyAwardEntity>queryRaffleStrategyAwardList(Long strategyId);
}
