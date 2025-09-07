package cn.bugstack.test.domain;

import cn.bugstack.domain.strategy.service.armory.IStrategyArmory;
import cn.bugstack.domain.strategy.service.armory.IStrategyDisPatch;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class StrategyArmoryTest {
    @Resource
    IStrategyArmory strategyArmory;
    @Resource
    IStrategyDisPatch strategyDisPatch;
    @Test
    public void test_strategy_armory() {
        strategyArmory.assembleLotteryStrategy(100002L);
    }
    @Test
    public void test_getrandomaward() {
        log.info("奖品ID{}",strategyDisPatch.getRandomAwardId(100002L));
    }

}
