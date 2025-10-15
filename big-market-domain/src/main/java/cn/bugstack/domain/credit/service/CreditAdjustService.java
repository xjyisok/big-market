package cn.bugstack.domain.credit.service;

import cn.bugstack.domain.credit.event.CreditAdjustSuccessMessageEvent;
import cn.bugstack.domain.credit.model.aggergate.TradeAggregate;
import cn.bugstack.domain.credit.model.entity.CreditAccountEntity;
import cn.bugstack.domain.credit.model.entity.CreditOrderEntity;
import cn.bugstack.domain.credit.model.entity.TaskEntity;
import cn.bugstack.domain.credit.model.entity.TradeEntity;
import cn.bugstack.domain.credit.repository.ICreditRepository;
import cn.bugstack.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class CreditAdjustService implements ICreditAdjustService {
    @Resource
    ICreditRepository creditRepository;
    @Resource
    private CreditAdjustSuccessMessageEvent creditAdjustSuccessMessageEvent;
    @Override
    public String createOrder(TradeEntity trade) {
        log.info("增加账户积分额度开始userId:{},tradeName:{},amount:{}", trade.getUserId(), trade.getTradeName(),trade.getAmount());
        //创建账户实体对象
        CreditAccountEntity creditAccountEntity = TradeAggregate.createCreditAccountEntity(
                trade.getUserId(),
                trade.getAmount());
        //创建订单实体对象
        CreditOrderEntity creditOrderEntity = TradeAggregate.createCreditOrderEntity(
                trade.getUserId(),
                trade.getTradeName(),
                trade.getTradeType(),
                trade.getAmount(),
                trade.getOutBusinessNo()
        );
        //构建任务对象
        CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage creditAdjustSuccessMessage = new CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage();
        creditAdjustSuccessMessage.setUserId(trade.getUserId());
        creditAdjustSuccessMessage.setOrderId(creditOrderEntity.getOrderId());
        creditAdjustSuccessMessage.setAmount(trade.getAmount());
        creditAdjustSuccessMessage.setOutBusinessNo(trade.getOutBusinessNo());
        BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> creditAdjustSuccessMessageEventMessage = creditAdjustSuccessMessageEvent.buildEventMessage(creditAdjustSuccessMessage);

        TaskEntity taskEntity = TradeAggregate.createTaskEntity(trade.getUserId(), creditAdjustSuccessMessageEvent.topic(), creditAdjustSuccessMessageEventMessage.getId(), creditAdjustSuccessMessageEventMessage);

        TradeAggregate tradeAggregate=new TradeAggregate();
        tradeAggregate.setCreditAccountEntity(creditAccountEntity);
        tradeAggregate.setCreditOrderEntity(creditOrderEntity);
        tradeAggregate.setTaskEntity(taskEntity);
        tradeAggregate.setUserId(trade.getUserId());
        //保存聚合对象到库表
        creditRepository.saveUserCreditTradeOrder(tradeAggregate);
        return null;
    }

    @Override
    public CreditAccountEntity queryUserCreditAccount(String userId) {
        return creditRepository.queryUserCreditAccount(userId);
    }
}
