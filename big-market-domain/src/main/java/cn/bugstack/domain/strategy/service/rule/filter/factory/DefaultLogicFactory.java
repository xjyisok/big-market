package cn.bugstack.domain.strategy.service.rule.filter.factory;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.service.annotation.LogicStrategy;
import cn.bugstack.domain.strategy.service.rule.filter.ILogicFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 规则工厂
 * @create 2023-12-31 11:23
 */
/*
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogicStrategy {

    DefaultLogicFactory.LogicModel logicMode();

}

 */
@Service
public class DefaultLogicFactory {

    public Map<String, ILogicFilter<?>> logicFilterMap = new ConcurrentHashMap<>();
    //这里是对springboot自动注入机制的运用
    /*
    那么，当 Spring 创建 DefaultLogicFactory 实例时，会先查看它的构造函数。
    发现这个构造函数需要一个 List<ILogicFilter<?>>，于是 Spring 会去容器里找所有实现了 ILogicFilter 的 Bean，并把它们自动组装成一个 List 传进来。
     */
    public DefaultLogicFactory(List<ILogicFilter<?>> logicFilters) {
        logicFilters.forEach(logic -> {
            LogicStrategy strategy = AnnotationUtils.findAnnotation(logic.getClass(), LogicStrategy.class);
            if (null != strategy) {
                logicFilterMap.put(strategy.logicMode().getCode(), logic);
            }
        });
    }

    public <T extends RuleActionEntity.RaffleEntity> Map<String, ILogicFilter<T>> openLogicFilter() {
        return (Map<String, ILogicFilter<T>>) (Map<?, ?>) logicFilterMap;
    }

    @Getter
    @AllArgsConstructor
    public enum LogicModel {

        RULE_WIGHT("rule_weight","【抽奖前规则】根据抽奖权重返回可抽奖范围KEY","before"),
        RULE_BLACKLIST("rule_blacklist","【抽奖前规则】黑名单规则过滤，命中黑名单则直接返回","before"),
        RULE_LOCK("rule_lock","抽奖中规则抽奖n次后对应奖品可以解锁抽奖","in"),
        RULE_LUCK_AWARD("rule_luck_award","抽奖后规则幸运架兜底","after"),
        ;

        private final String code;
        private final String info;
        private final String state;

        public static boolean isIn(String rule_value){
            return "in".equals(LogicModel.valueOf(rule_value.toUpperCase()).state);
        }
        public static boolean isAfter(String rule_value){
            return "after".equals(LogicModel.valueOf(rule_value.toUpperCase()).state);
        }
    }

}

