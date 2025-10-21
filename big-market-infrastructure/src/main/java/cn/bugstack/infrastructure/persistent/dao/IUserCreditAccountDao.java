package cn.bugstack.infrastructure.persistent.dao;

import cn.bugstack.infrastructure.persistent.po.UserCreditAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserCreditAccountDao {
    int updateAddAmount(UserCreditAccount userCreditAccount);

    void insert(UserCreditAccount userCreditAccount);

    UserCreditAccount queryUserCreditAccount(UserCreditAccount userCreditAccount);

    int updateSubtractionAmount(UserCreditAccount userCreditAccountReq);
}
