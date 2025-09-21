package cn.bugstack.domain.activity.service.partake;

import cn.bugstack.domain.activity.model.aggerate.CreatePartakeOrderAggregate;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.PartakeRaffleActivityEntity;
import cn.bugstack.domain.activity.model.entity.UserRaffleOrderEntity;
import cn.bugstack.domain.activity.model.valobj.ActivityStateVO;
import cn.bugstack.domain.activity.respository.IActivityRespository;
import cn.bugstack.domain.activity.service.IRaffleActivityPartakeService;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * 抽奖活动参与服务
 */
@Slf4j
public abstract class AbstractRaffleActivityPartake implements IRaffleActivityPartakeService {
    protected final IActivityRespository activityRespository;
    public AbstractRaffleActivityPartake(IActivityRespository activityRespository) {
        this.activityRespository = activityRespository;
    }
    @Override
    public UserRaffleOrderEntity createOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity) {
        String userId=partakeRaffleActivityEntity.getUserId();
        Long activityId=partakeRaffleActivityEntity.getActivityId();
        Date currentDate=new Date();
        //查询活动实体
        ActivityEntity activityEntity=activityRespository.queryRaffleActivityByActivityId(activityId);
        //基础性息过滤
        if(!ActivityStateVO.open.getCode().equals(activityEntity.getState().getCode())){
            throw new AppException(ResponseCode.ACTIVITY_STATE_ERROR.getCode(),ResponseCode.ACTIVITY_STATE_ERROR.getInfo());
        }
        if(activityEntity.getBeginDateTime().after(currentDate)||activityEntity.getEndDateTime().before(currentDate)){
            throw new AppException(ResponseCode.ACTIVITY_DATE_ERROR.getCode(),ResponseCode.ACTIVITY_DATE_ERROR.getInfo());
        }
        //判断是否有未消费的订单
        UserRaffleOrderEntity userRaffleOrderEntity=activityRespository.queryNoUsedRaffleOrder(partakeRaffleActivityEntity);
        if(userRaffleOrderEntity!=null){
            log.info("存在未消费的抽奖订单userId:{},activityId:{} userorderentity:{}",userId,activityId, JSON.toJSONString(userRaffleOrderEntity));
            return userRaffleOrderEntity;
        }
        CreatePartakeOrderAggregate createPartakeOrderAggregate=doFilterAccount(userId,activityId,currentDate);
        UserRaffleOrderEntity userRaffleOrderEntity1=builderUserRaffleOrder(userId,activityId,currentDate);
        createPartakeOrderAggregate.setUserRaffleOrderEntity(userRaffleOrderEntity1);
        activityRespository.saveCreatePartakeOrderAggerate(createPartakeOrderAggregate);
        return userRaffleOrderEntity1;
    }
    protected abstract CreatePartakeOrderAggregate doFilterAccount(String userId,Long activityId,Date currentDate);
    protected abstract UserRaffleOrderEntity builderUserRaffleOrder(String userId,Long activityId,Date currentDate);
}
