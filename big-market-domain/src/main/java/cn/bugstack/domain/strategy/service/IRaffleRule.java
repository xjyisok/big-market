package cn.bugstack.domain.strategy.service;

import java.util.Map;

public interface IRaffleRule {
    public Map<String,Integer>queryAwardRuleLockCount(String[] treeIds);
}
