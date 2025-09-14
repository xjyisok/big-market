package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.respository.IActivityRespository;
import org.springframework.stereotype.Service;

@Service
public class RaffleActivityService extends AbstractRaffleActivity{
    public RaffleActivityService(IActivityRespository activityRepository) {
        super(activityRepository);
    }
}
