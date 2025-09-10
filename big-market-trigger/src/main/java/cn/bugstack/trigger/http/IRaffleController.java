package cn.bugstack.trigger.http;

import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactoryEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.service.IRaffleAward;
import cn.bugstack.domain.strategy.service.IRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyArmory;
import cn.bugstack.trigger.api.IRaffleService;
import cn.bugstack.trigger.api.dto.RaffleAwardListRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleAwardListResponseDTO;
import cn.bugstack.trigger.api.dto.RaffleRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleResponseDTO;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.model.Response;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/")
public class IRaffleController implements IRaffleService {
    @Resource
    private IStrategyArmory strategyArmory;
    @Resource
    private IRaffleAward raffleAward;
    @Resource
    private IRaffleStrategy raffleStrategy;
    @Override
    @RequestMapping(value="query_raffle_award_list",method = RequestMethod.POST)
    public Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(@RequestBody RaffleAwardListRequestDTO req) {
        try{
            log.info("奖品列表查询开始 strategyId:{}", req.getStrategyId());
            List<RaffleAwardListResponseDTO>raffleAwardListResponseDTOList=new ArrayList<RaffleAwardListResponseDTO>();
            List<StrategyAwardEntity>strategyAwardEntityList=raffleAward.queryRaffleStrategyAwardList(req.getStrategyId());
            for(StrategyAwardEntity strategyAwardEntity:strategyAwardEntityList){
                raffleAwardListResponseDTOList.add(RaffleAwardListResponseDTO
                        .builder()
                        .awardId(strategyAwardEntity.getAwardId())
                        .awardTitle(strategyAwardEntity.getAwardTitle())
                        .awardSubtitle(strategyAwardEntity.getAwardSubtitle())
                        .sort(strategyAwardEntity.getSort())
                        .build());
            }
            log.info("奖品列表查询结束 strategyId:{}", req.getStrategyId());
            return Response.<List<RaffleAwardListResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(raffleAwardListResponseDTOList)
                    .build();
        }catch(Exception e){
            log.error("奖品列表查询异常 strategyId:{}", req.getStrategyId());
            return Response.<List<RaffleAwardListResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
    @RequestMapping(value = "strategy_armory",method= RequestMethod.GET)
    @Override
    public Response<Boolean> strategyArmory(Long strategyId) {
        try {
            log.info("抽奖策略装配开始 strategyId:{}", strategyId);
            boolean armorystatus=strategyArmory.assembleLotteryStrategy(strategyId);
            Response<Boolean> response=Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(armorystatus)
                    .build();
            log.info("抽奖策略装配成功 strategyId:{} response：{}", strategyId, JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.error("抽奖策略装配失败 strategyId:{}", strategyId);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
            //throw new RuntimeException(e);
        }
    }

    @Override
    @RequestMapping(value = "random_raffle",method= RequestMethod.POST)
    public Response<RaffleResponseDTO> randomeRaffle(@RequestBody RaffleRequestDTO req) {
        try {
            log.info("抽奖开始 strategyId:{}", req.getStrategyId());
            RaffleAwardEntity raffleAwardEntity=raffleStrategy.performRaffle(RaffleFactoryEntity.builder()
                    .userId("system")
                    .strategyId(req.getStrategyId())
                    .build());
            Response<RaffleResponseDTO>response=Response.<RaffleResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(RaffleResponseDTO.builder()
                            .awardId(raffleAwardEntity.getAwardId())
                            .awardIndex(raffleAwardEntity.getSort())
                            .build())
                    .build();
            log.info("抽奖完成 strategyId:{} response：{}", req.getStrategyId(),JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.error("抽奖异常 strategyId:{} err:{}", req.getStrategyId(),e.getMessage());
            //throw new RuntimeException(e);
            return Response.<RaffleResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
}
