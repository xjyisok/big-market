package cn.bugstack.domain.award.service.distribute;

import cn.bugstack.domain.award.model.entity.DistributeAwardEntity;

public interface IDistributeAward {
    public void giveOutPrizes(DistributeAwardEntity distributeAwardEntity);
}
