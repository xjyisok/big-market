package cn.bugstack.test.domain.credit;

import cn.bugstack.domain.credit.model.entity.TradeEntity;
import cn.bugstack.domain.credit.model.valobj.TradeNameVO;
import cn.bugstack.domain.credit.model.valobj.TradeTypeVO;
import cn.bugstack.domain.credit.service.CreditAdjustService;
import cn.bugstack.domain.credit.service.ICreditAdjustService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class CreditAdjustServiceTest {
    @Resource
    private ICreditAdjustService creditAdjustService;
    @Test
    public void test_CreateOrder_forward(){
        TradeEntity trade = new TradeEntity();
        trade.setUserId("xiaofuge");
        trade.setTradeName(TradeNameVO.REBATE);
        trade.setTradeType(TradeTypeVO.FORWARD);
        trade.setAmount(new BigDecimal("10"));
        trade.setOutBusinessNo("100000000000");
        creditAdjustService.createOrder(trade);
    }
    @Test
    public void test_createOrder_pay() throws InterruptedException {
        TradeEntity tradeEntity = new TradeEntity();
        tradeEntity.setUserId("xiaofuge");
        tradeEntity.setTradeName(TradeNameVO.CONVERT_SKU);
        tradeEntity.setTradeType(TradeTypeVO.REVERSE);
        tradeEntity.setAmount(new BigDecimal("-1.68"));
        tradeEntity.setOutBusinessNo("70009240614001");
        creditAdjustService.createOrder(tradeEntity);

        new CountDownLatch(1).await();
    }

}
