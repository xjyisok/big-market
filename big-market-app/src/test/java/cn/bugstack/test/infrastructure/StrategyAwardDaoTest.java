package cn.bugstack.test.infrastructure;

import cn.bugstack.infrastructure.persistent.dao.IAwardDao;
import cn.bugstack.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.bugstack.infrastructure.persistent.po.Award;
import cn.bugstack.infrastructure.persistent.po.StrategyAward;
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
public class StrategyAwardDaoTest {
    @Autowired
    IStrategyAwardDao strategyAwardDao;
    @Test
    public void test_queryStrategyAwardList(){
        List<StrategyAward> strategyawardList = strategyAwardDao.queryStrategyAwardList();
        System.out.println(strategyawardList.size());
        log.info(strategyawardList.toString());
    }
}
