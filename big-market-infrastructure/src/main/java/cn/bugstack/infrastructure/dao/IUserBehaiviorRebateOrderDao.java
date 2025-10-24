package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.UserBehaviorRebateOrder;
import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.bugstack.middleware.db.router.annotation.DBRouterStrategy;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DBRouterStrategy(splitTable = true)
public interface IUserBehaiviorRebateOrderDao {
    void insert(UserBehaviorRebateOrder userBehaviorRebateOrder);
    @DBRouter
    List<UserBehaviorRebateOrder> queryRebateOrderByOutBusinessId(UserBehaviorRebateOrder userBehaviorRebateOrder);
}
