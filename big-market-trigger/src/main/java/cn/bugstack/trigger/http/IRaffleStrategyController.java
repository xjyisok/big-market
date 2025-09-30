package cn.bugstack.trigger.http;

import cn.bugstack.domain.activity.service.IRaffleActivityPartakeService;
import cn.bugstack.domain.activity.service.IRaffleOrderAccountQuoat;
import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactoryEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.vo.RuleWeightVO;
import cn.bugstack.domain.strategy.service.IRaffleAward;
import cn.bugstack.domain.strategy.service.IRaffleRule;
import cn.bugstack.domain.strategy.service.IRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyArmory;

import cn.bugstack.trigger.api.IRaffleStrategyService;
import cn.bugstack.trigger.api.dto.*;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import cn.bugstack.types.model.Response;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/strategy")
public class IRaffleStrategyController implements IRaffleStrategyService {
    @Resource
    private IStrategyArmory strategyArmory;
    @Resource
    private IRaffleAward raffleAward;
    @Resource
    private IRaffleRule raffleRule;
    @Resource
    private IRaffleStrategy raffleStrategy;
    @Resource
    private IRaffleActivityPartakeService raffleActivityPartakeService;
    @Resource
    IRaffleOrderAccountQuoat raffleOrderAccountQuoat;
    @Override
    @RequestMapping(value="query_raffle_award_list",method = RequestMethod.POST)
    public Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(@RequestBody RaffleAwardListRequestDTO req) {
        try{
            if(StringUtils.isBlank(req.getUserId()) ||req.getActivityId()==null){
                return Response.<List<RaffleAwardListResponseDTO>>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }
            log.info("奖品列表查询开始 userId:{} activityId:{}", req.getUserId(),req.getActivityId());
            List<RaffleAwardListResponseDTO>raffleAwardListResponseDTOList=new ArrayList<RaffleAwardListResponseDTO>();
            //查询奖品配置
            List<StrategyAwardEntity>strategyAwardEntityList=raffleAward.queryRaffleStrategyAwardListByActivityId(req.getActivityId());
            //System.out.println(strategyAwardEntityList);
            String[] treeIds=strategyAwardEntityList.stream().map(StrategyAwardEntity::getRuleModels)
                    .filter(ruleModel->ruleModel!=null&&!ruleModel.isEmpty())
                    .toArray(String[]::new);
            //查询规则配置-获取奖品的解锁限制抽奖N次后解锁
            Map<String,Integer>rulelockcount=raffleRule.queryAwardRuleLockCount(treeIds);
            //查询抽奖次数-用户已经参与抽奖的次数
            Integer dayPartakeCount=raffleActivityPartakeService.queryRaffleActivityAccountDayPartakeCount(req.getUserId(),req.getActivityId());
            //遍历填充数据
            for(StrategyAwardEntity strategyAwardEntity:strategyAwardEntityList){
                Integer rulelockvalue=rulelockcount.get(strategyAwardEntity.getRuleModels());
                raffleAwardListResponseDTOList.add(RaffleAwardListResponseDTO
                        .builder()
                        .awardId(strategyAwardEntity.getAwardId())
                        .awardTitle(strategyAwardEntity.getAwardTitle())
                        .awardSubtitle(strategyAwardEntity.getAwardSubtitle())
                        .sort(strategyAwardEntity.getSort())
                                .awardRuleLockCount(rulelockvalue)
                                .isAwardUnlock(rulelockvalue==null||dayPartakeCount>=rulelockvalue)
                        .waitUnlockCount(rulelockvalue==null||dayPartakeCount>=rulelockvalue?0:rulelockvalue-dayPartakeCount)
                        .build());
            }
            log.info("奖品列表查询结束 userId:{} activityId:{}", req.getUserId(),req.getActivityId());
            return Response.<List<RaffleAwardListResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(raffleAwardListResponseDTOList)
                    .build();
        }catch(Exception e){
            log.error("奖品列表查询异常 userId:{} activityId:{}", req.getUserId(),req.getActivityId(),e);
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
    @RequestMapping(value = "query_raffle_strategy_rule_weight", method = RequestMethod.POST)
    @Override
    public Response<List<RaffleStrategyRuleWeightResponseDTO>> queryRaffleStrategyRuleWeight(RaffleStrategyRuleWeightRequestDTO request) {
        try {
            log.info("查询抽奖策略权重规则配置开始 userId:{} activityId：{}", request.getUserId(), request.getActivityId());
            // 1. 参数校验
            if (StringUtils.isBlank(request.getUserId()) || null == request.getActivityId()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 查询用户抽奖总次数
            Integer userActivityAccountTotalUseCount = raffleOrderAccountQuoat.queryRaffleActivityAccountPartakeCount(request.getActivityId(), request.getUserId());
            // 3. 查询规则
            List<RaffleStrategyRuleWeightResponseDTO> raffleStrategyRuleWeightList = new ArrayList<>();
            List<RuleWeightVO> ruleWeightVOList = raffleRule.queryAwardRuleWeightByActivityId(request.getActivityId());
            for (RuleWeightVO ruleWeightVO : ruleWeightVOList) {
                // 转换对象
                List<RaffleStrategyRuleWeightResponseDTO.StrategyAward> strategyAwards = new ArrayList<>();
                List<RuleWeightVO.Award> awardList = ruleWeightVO.getAwardList();
                for (RuleWeightVO.Award award : awardList) {
                    RaffleStrategyRuleWeightResponseDTO.StrategyAward strategyAward = new RaffleStrategyRuleWeightResponseDTO.StrategyAward();
                    strategyAward.setAwardId(award.getAwardId());
                    strategyAward.setAwardTitle(award.getAwardTitle());
                    strategyAwards.add(strategyAward);
                }
                // 封装对象
                RaffleStrategyRuleWeightResponseDTO raffleStrategyRuleWeightResponseDTO = new RaffleStrategyRuleWeightResponseDTO();
                raffleStrategyRuleWeightResponseDTO.setRuleWeightCount(ruleWeightVO.getWeight());
                raffleStrategyRuleWeightResponseDTO.setStrategyAwards(strategyAwards);
                raffleStrategyRuleWeightResponseDTO.setUserActivityAccountTotalUseCount(userActivityAccountTotalUseCount);

                raffleStrategyRuleWeightList.add(raffleStrategyRuleWeightResponseDTO);
            }
            Response<List<RaffleStrategyRuleWeightResponseDTO>> response = Response.<List<RaffleStrategyRuleWeightResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(raffleStrategyRuleWeightList)
                    .build();
            log.info("查询抽奖策略权重规则配置完成 userId:{} activityId：{} response: {}", request.getUserId(), request.getActivityId(), JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.error("查询抽奖策略权重规则配置失败 userId:{} activityId：{}", request.getUserId(), request.getActivityId(), e);
            return Response.<List<RaffleStrategyRuleWeightResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @Override
    @RequestMapping(value = "random_raffle",method= RequestMethod.POST)
    public Response<RaffleStrategyResponseDTO> randomeRaffle(@RequestBody RaffleStrategyRequestDTO req) {
        try {
            log.info("抽奖开始 strategyId:{}", req.getStrategyId());
            RaffleAwardEntity raffleAwardEntity=raffleStrategy.performRaffle(RaffleFactoryEntity.builder()
                    .userId("system")
                    .strategyId(req.getStrategyId())
                    .build());
            Response<RaffleStrategyResponseDTO>response=Response.<RaffleStrategyResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(RaffleStrategyResponseDTO.builder()
                            .awardId(raffleAwardEntity.getAwardId())
                            .awardIndex(raffleAwardEntity.getSort())
                            .build())
                    .build();
            log.info("抽奖完成 strategyId:{} response：{}", req.getStrategyId(),JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.error("抽奖异常 strategyId:{} err:{}", req.getStrategyId(),e.getMessage());
            //throw new RuntimeException(e);
            return Response.<RaffleStrategyResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
}
