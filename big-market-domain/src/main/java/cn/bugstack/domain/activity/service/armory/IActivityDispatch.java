package cn.bugstack.domain.activity.service.armory;

import java.util.Date;

/**
 * 库存扣减操作
 */
public interface IActivityDispatch {
    public boolean subtarctionActivitySkuStock(Long sku, Date endDateTime);
}
