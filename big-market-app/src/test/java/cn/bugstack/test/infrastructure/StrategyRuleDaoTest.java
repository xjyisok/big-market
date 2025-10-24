package cn.bugstack.test.infrastructure;

import cn.bugstack.infrastructure.dao.IStrategyRuleDao;
import cn.bugstack.infrastructure.dao.po.StrategyRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/*
奖品持久化单元测试
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class StrategyRuleDaoTest {
    @Autowired
    IStrategyRuleDao strategyRuleDao;
    @Test
    public void test_queryAwardList(){
        List<StrategyRule> strategyRulesList = strategyRuleDao.queryStrategyRuleList();
        log.info(strategyRulesList.toString());
        System.out.println(strategyRulesList.size());
    }
}
