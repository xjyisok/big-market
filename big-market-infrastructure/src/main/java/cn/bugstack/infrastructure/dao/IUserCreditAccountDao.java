package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.UserCreditAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserCreditAccountDao {
    int updateAddAmount(UserCreditAccount userCreditAccount);

    void insert(UserCreditAccount userCreditAccount);

    UserCreditAccount queryUserCreditAccount(UserCreditAccount userCreditAccount);

    int updateSubtractionAmount(UserCreditAccount userCreditAccountReq);
}
