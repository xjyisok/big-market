package cn.bugstack.trigger.api;

import cn.bugstack.trigger.api.dto.*;
import cn.bugstack.types.model.Response;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动服务
 * @create 2024-04-13 09:16
 */
public interface IRaffleActivityService {

    /**
     * 活动装配，数据预热缓存
     * @param activityId 活动ID
     * @return 装配结果
     */
    Response<Boolean> armory(Long activityId);

    /**
     * 活动抽奖接口
     * @param request 请求对象
     * @return 返回结果
     */
    Response<ActivityDrawResponseDTO> draw(ActivityDrawRequestDTO request);
    public Response<Boolean> calendarSignRebate(String userId);
    Response<Boolean>isCalendarSignRebate(String userId);
    Response<UserActivityAccountResponseDTO>queryUserActivityAccount(UserActivityAccountRequestDTO request);
    /**
     * 查询sku商品集合
     *
     * @param activityId 活动ID
     * @return 商品集合
     */
    Response<List<SkuProductResponseDTO>> querySkuProductListByActivityId(Long activityId);

    /**
     * 查询用户积分值
     *
     * @param userId 用户ID
     * @return 可用积分
     */
    Response<BigDecimal> queryUserCreditAccount(String userId);

    /**
     * 积分支付兑换商品
     *
     * @param request 请求对象「用户ID、商品ID」
     * @return 兑换结果
     */
    Response<Boolean> creditPayExchangeSku(SkuProductShopCartRequestDTO request);

}
