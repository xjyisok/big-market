package cn.bugstack.trigger.api;

import cn.bugstack.trigger.api.dto.RaffleAwardListRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleAwardListResponseDTO;
import cn.bugstack.trigger.api.dto.RaffleRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleResponseDTO;
import cn.bugstack.types.model.Response;

import java.util.List;

/*
抽奖服务接口
 */
public interface IRaffleService {
    Response<Boolean> strategyArmory(Long strategyId);
    Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(RaffleAwardListRequestDTO req);
    Response<RaffleResponseDTO>randomeRaffle(RaffleRequestDTO req);
}
