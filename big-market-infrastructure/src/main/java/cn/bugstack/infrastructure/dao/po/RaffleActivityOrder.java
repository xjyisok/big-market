package cn.bugstack.infrastructure.dao.po;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动单 持久化对象
 * @create 2024-03-02 13:21
 */
@Data
public class RaffleActivityOrder {

    /**
     * 自增ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */

    private Long activityId;
    /**
     * sku
     */
    private Long sku;
    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 抽奖策略ID
     */
    private Long strategyId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 下单时间
     */
    private Date orderTime;
    /**
     * 总次数
     */
    private Integer totalCount;
    /**
     * 月次数
     */
    private Integer monthCount;
    /**
     * 日次数
     */
    private Integer dayCount;

    /**
     * 订单状态（not_used、used、expire）
     */
    private String state;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 唯一Id防止重复添加次数
     */
    private String outBusinessNo;
    /**
     * 商品金额【积分值】
     */
    private BigDecimal payAmount;
}
