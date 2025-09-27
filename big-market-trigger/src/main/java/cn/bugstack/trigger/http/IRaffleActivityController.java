package cn.bugstack.trigger.http;

import cn.bugstack.domain.activity.model.entity.UserRaffleOrderEntity;
import cn.bugstack.domain.activity.service.IRaffleActivityPartakeService;
import cn.bugstack.domain.activity.service.armory.IActivityArmory;
import cn.bugstack.domain.award.model.entity.UserAwardRecordEntity;
import cn.bugstack.domain.award.model.valobj.AwardSendStateVO;
import cn.bugstack.domain.award.service.IAwardService;
import cn.bugstack.domain.rebate.model.entity.BehaviorEntity;
import cn.bugstack.domain.rebate.model.valobj.BehaviorTypeVO;
import cn.bugstack.domain.rebate.service.IBehaviorRebateService;
import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactoryEntity;
import cn.bugstack.domain.strategy.service.IRaffleAward;
import cn.bugstack.domain.strategy.service.IRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyArmory;
import cn.bugstack.trigger.api.IRaffleActivityService;
import cn.bugstack.trigger.api.IRaffleStrategyService;
import cn.bugstack.trigger.api.dto.ActivityDrawRequestDTO;
import cn.bugstack.trigger.api.dto.ActivityDrawResponseDTO;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import cn.bugstack.types.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/activity")
public class IRaffleActivityController implements IRaffleActivityService {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    @Resource
    IActivityArmory activityArmory;
    @Resource
    IStrategyArmory strategyArmory;
    @Resource
    IRaffleActivityPartakeService activityPartakeService;
    @Resource
    IRaffleStrategy raffleStrategy;
    @Resource
    IAwardService awardService;
    @Resource
    IBehaviorRebateService behaviorRebateService;
    @RequestMapping(value = "/armory",method = RequestMethod.GET)
    @Override
    public Response<Boolean> armory(@RequestParam Long activityId) {
        try{
            log.info("装配开始，数据预热 activity id {}", activityId);
            activityArmory.assembleActivitySkuByActivityId(activityId);
            strategyArmory.assembleLotteryStrategyByActivityId(activityId);
            Response<Boolean>response=Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
            log.info("装配结束activity id {}", activityId);
            return response;
        }catch(Exception e){
            log.error("装配失败 activity id {}", activityId, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }
    @RequestMapping(value = "/draw",method = RequestMethod.POST)
    @Override
    public Response<ActivityDrawResponseDTO> draw(@RequestBody ActivityDrawRequestDTO request) {
        try{
            log.info("活动抽奖 activity id {} userId {}", request.getActivityId(),request.getUserId());
        if(StringUtils.isBlank(request.getUserId())||request.getActivityId()==null){
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        //创建抽奖单
        UserRaffleOrderEntity userRaffleOrderEntity=activityPartakeService.createOrder(request.getActivityId(),request.getUserId());
        log.info("开始抽奖 activity id {} userId {} orderId {}", request.getActivityId(),request.getUserId(),userRaffleOrderEntity.getOrderId());
        //执行抽奖
        RaffleAwardEntity raffleAwardEntity=raffleStrategy.performRaffle(RaffleFactoryEntity.builder()
                        .strategyId(userRaffleOrderEntity.getStrategyId())
                        .userId(userRaffleOrderEntity.getUserId())
                        .endDateTime(userRaffleOrderEntity.getEndDateTime())
                .build());
            UserAwardRecordEntity userAwardRecord = UserAwardRecordEntity.builder()
                    .userId(userRaffleOrderEntity.getUserId())
                    .activityId(userRaffleOrderEntity.getActivityId())
                    .strategyId(userRaffleOrderEntity.getStrategyId())
                    .orderId(userRaffleOrderEntity.getOrderId())
                    .awardId(raffleAwardEntity.getAwardId())
                    .awardTitle(raffleAwardEntity.getAwardTitle())
                    .awardTime(new Date())
                    .awardState(AwardSendStateVO.create)
                    .build();
            System.out.println(userAwardRecord.getOrderId()+"=------------------------------------------------");
            //保存抽奖记录
            awardService.saveUserAwardRecord(userAwardRecord);
            // 5. 返回结果
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(ActivityDrawResponseDTO.builder()
                            .awardId(raffleAwardEntity.getAwardId())
                            .awardTitle(raffleAwardEntity.getAwardTitle())
                            .awardIndex(raffleAwardEntity.getSort())
                            .build())
                    .build();

        }catch (AppException e){
            log.error("活动抽奖失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();

        }catch (Exception e){
            log.error("活动抽奖失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();

        }
    }

    @Override
    @RequestMapping(value = "/calendar_sign_rebate",method = RequestMethod.POST)
    public Response<Boolean> calendarSignRebate(String userId) {
        try {
            BehaviorEntity behaviorEntity = new BehaviorEntity();
            behaviorEntity.setUserId(userId);
            behaviorEntity.setBehaviorTypeVO(BehaviorTypeVO.SIGN);
            behaviorEntity.setOutBusinessNo(dateFormat.format(new Date()));
            List<String>orserIds=behaviorRebateService.createOrder(behaviorEntity);
            log.info("日历签到返利完成userID:{},orderIds:{}", userId, orserIds);
            return  Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
        }catch (AppException e){
            log.error("返利订单创建异常userID:{}", userId, e);
            return Response.<Boolean>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .data(false)
                    .build();
        }catch (Exception e){
            log.error("未知错误userID:{}", userId, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }
}
