package cn.bugstack.domain.strategy.service.armory.algorithm;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.respository.IStrategyRespository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.Resource;
import java.util.List;

public abstract class AbstractAlgorithm implements IAlgorithm {
    @Resource
    public IStrategyRespository strategyRespository;
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public enum AlgorithmType {
        O1("o1"),
        OLogN("oLogN");
        private  String key;
    }
}
