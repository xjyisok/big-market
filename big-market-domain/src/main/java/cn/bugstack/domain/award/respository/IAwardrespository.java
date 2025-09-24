package cn.bugstack.domain.award.respository;

import cn.bugstack.domain.award.model.aggregate.UserAwardRecordAggregate;

public interface IAwardrespository {
    void saveUserAwardRecordAggerate(UserAwardRecordAggregate userAwardRecordAggregate);
}
