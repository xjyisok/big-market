package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.Strategy;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IStrategyDao {
    List<Strategy> queryStrategyList();
    Strategy queryStrategyListById(Long strategyId);

}
