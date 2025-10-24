package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.StrategyAward;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IStrategyAwardDao {
    void updateStrategyAwardStock(StrategyAward strategyAward) ;

    List<StrategyAward> queryStrategyAwardList();
    List<StrategyAward> queryStrategyAwardListById(Long StrategyAwardId);

    StrategyAward queryStrategyAward(StrategyAward strategyAward);
}
