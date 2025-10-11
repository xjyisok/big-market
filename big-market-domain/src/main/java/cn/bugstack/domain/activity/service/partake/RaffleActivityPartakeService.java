package cn.bugstack.domain.activity.service.partake;

import cn.bugstack.domain.activity.model.aggerate.CreatePartakeOrderAggregate;
import cn.bugstack.domain.activity.model.entity.*;
import cn.bugstack.domain.activity.model.valobj.UserRaffleOrderStateVO;
import cn.bugstack.domain.activity.respository.IActivityRespository;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
@Service
public class RaffleActivityPartakeService extends AbstractRaffleActivityPartake{
    private final SimpleDateFormat dateFormatMonth = new SimpleDateFormat("yyyy-MM");
    private final SimpleDateFormat dateFormatDay = new SimpleDateFormat("yyyy-MM-dd");
    public RaffleActivityPartakeService(IActivityRespository activityRespository) {
        super(activityRespository);
    }

    @Override
    protected CreatePartakeOrderAggregate doFilterAccount(String userId, Long activityId, Date currentDate) {
        ActivityAccountEntity activityAccountEntity=activityRespository.queryActivityAccountByUserId(userId,activityId);
        if(activityAccountEntity==null||activityAccountEntity.getTotalCountSurplus()==0){
            throw new AppException(ResponseCode.ACTIVITY_QUOTA_ERROR.getCode(),ResponseCode.ACTIVITY_QUOTA_ERROR.getInfo());
        }
        String month=dateFormatMonth.format(currentDate);
        ActivityAccountMonthEntity activityAccountMonthEntity=activityRespository.queryActivityAccountMonthByUserId(userId,activityId,month);
        if(null!=activityAccountMonthEntity&&activityAccountMonthEntity.getMonthCountSurplus()<=0){
            throw new AppException(ResponseCode.ACTIVITY_MONTH_QUOTA_ERROR.getCode(),ResponseCode.ACTIVITY_MONTH_QUOTA_ERROR.getInfo());
        }//由于所有的账户扣减都是在镜像账户中执行，镜像账户在用户在每天/每月更新的时候，当天/月的镜像账户中是没有用户信息的所以要在镜像表中插入。
        boolean isMonthAccountexisted=null!=activityAccountMonthEntity;
        if(null==activityAccountMonthEntity){
            activityAccountMonthEntity=new ActivityAccountMonthEntity();
            activityAccountMonthEntity.setUserId(userId);
            activityAccountMonthEntity.setActivityId(activityId);
            //TODO理解一下为什么是总量二不是剩余量
            activityAccountMonthEntity.setMonthCountSurplus(activityAccountEntity.getMonthCount());
            activityAccountMonthEntity.setMonthCount(activityAccountEntity.getMonthCount());
            activityAccountMonthEntity.setMonth(month);
        }
        String day=dateFormatDay.format(currentDate);
        ActivityAccountDayEntity activityAccountDayEntity=activityRespository.queryActivityAccountDayByUserId(userId,activityId,day);
        if(null!=activityAccountDayEntity&&activityAccountDayEntity.getDayCountSurplus()<=0){
            throw new AppException(ResponseCode.ACTIVITY_DAY_QUOTA_ERROR.getCode(),ResponseCode.ACTIVITY_DAY_QUOTA_ERROR.getInfo());
        }
        boolean isDayAccountexisted=null!=activityAccountDayEntity;
        if(null==activityAccountDayEntity){
            activityAccountDayEntity=new ActivityAccountDayEntity();
            activityAccountDayEntity.setUserId(userId);
            activityAccountDayEntity.setActivityId(activityId);
            //TODO理解一下为什么是总量二不是剩余量
            activityAccountDayEntity.setDayCountSurplus(activityAccountEntity.getDayCount());
            activityAccountDayEntity.setDayCount(activityAccountEntity.getDayCount());
            activityAccountDayEntity.setDay(day);
        }
        CreatePartakeOrderAggregate createPartakeOrderAggregate=new CreatePartakeOrderAggregate();
        createPartakeOrderAggregate.setUserId(userId);
        createPartakeOrderAggregate.setActivityId(activityId);
        createPartakeOrderAggregate.setActivityAccountEntity(activityAccountEntity);
        createPartakeOrderAggregate.setActivityAccountMonthEntity(activityAccountMonthEntity);
        createPartakeOrderAggregate.setActivityAccountDayEntity(activityAccountDayEntity);
        createPartakeOrderAggregate.setExistAccountMonth(isMonthAccountexisted);
        createPartakeOrderAggregate.setExistAccountDay(isDayAccountexisted);
        return createPartakeOrderAggregate;
    }

    @Override
    protected UserRaffleOrderEntity builderUserRaffleOrder(String userId, Long activityId, Date currentDate) {
        ActivityEntity activityEntity=activityRespository.queryRaffleActivityByActivityId(activityId);
        return UserRaffleOrderEntity.builder()
                .userId(userId)
                .activityId(activityId)
                .activityName(activityEntity.getActivityName())
                .strategyId(activityEntity.getStrategyId())
                .orderId(RandomStringUtils.randomNumeric(12))
                .orderTime(currentDate)
                .orderState(UserRaffleOrderStateVO.create)
                .endDateTime(activityEntity.getEndDateTime())
                .build();
    }

    @Override
    public Integer queryRaffleActivityAccountDayPartakeCount(String userId, Long activityId) {
        return activityRespository.queryRaffleActivityAccountDayPartakeCount(userId,activityId);
    }

}
