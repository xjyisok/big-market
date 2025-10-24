package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.RaffleActivitySku;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

//商品sku操作
@Mapper
public interface IRaffleActivitySkuDao {
    public RaffleActivitySku queryRaffleActivitySku(Long sku);

    void updateActivitySkuStock(Long sku);

    void clearActivitySkuStock(Long sku);

    List<RaffleActivitySku> queryActivitySkuListByActivityId(Long activityId);
}
