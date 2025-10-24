package cn.bugstack.infrastructure.dao.po;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class RaffleActivitySku {
    /**
     * 自增ID
     */
    private Long id;
    /**
     * sku
     */
    private Long sku;
    /**
     * 活动ID
     */
    private Long activityId;
    /**
     * 活动个人参与次数Id
     */
    private Long activityCountId;
    /**
     *商品库存
     */
    private Integer stockCount;
    /**
     * 剩余库存
     */
    private Integer stockCountSurplus;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 商品金额【积分值】
     */
    private BigDecimal productAmount;
    /**
     * 更新时间
     */
    private Date updateTime;

}
