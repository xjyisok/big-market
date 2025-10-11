package cn.bugstack.trigger.api;

import cn.bugstack.trigger.api.dto.*;
import cn.bugstack.types.model.Response;

import java.util.List;

/*
抽奖服务接口
 */
public interface IRaffleStrategyService {
    Response<Boolean> strategyArmory(Long strategyId);
    Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(RaffleAwardListRequestDTO req);
    Response<RaffleStrategyResponseDTO>randomeRaffle(RaffleStrategyRequestDTO req);
    public Response<List<RaffleStrategyRuleWeightResponseDTO>> queryRaffleStrategyRuleWeight(RaffleStrategyRuleWeightRequestDTO request);
}
