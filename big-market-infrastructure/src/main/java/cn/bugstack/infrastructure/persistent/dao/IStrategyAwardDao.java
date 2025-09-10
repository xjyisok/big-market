package cn.bugstack.infrastructure.persistent.dao;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.infrastructure.persistent.po.StrategyAward;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IStrategyAwardDao {
    void updateStrategyAwardStock(StrategyAward strategyAward) ;

    List<StrategyAward> queryStrategyAwardList();
    List<StrategyAward> queryStrategyAwardListById(Long StrategyAwardId);

    StrategyAward queryStrategyAward(StrategyAward strategyAward);
}
