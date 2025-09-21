package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.PartakeRaffleActivityEntity;
import cn.bugstack.domain.activity.model.entity.UserRaffleOrderEntity;

/**
 * @description 抽奖活动参与活动
 */
public interface IRaffleActivityPartakeService {
    UserRaffleOrderEntity createOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity);
}
