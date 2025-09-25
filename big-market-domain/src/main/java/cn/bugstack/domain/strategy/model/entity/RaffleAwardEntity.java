package cn.bugstack.domain.strategy.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleAwardEntity {
    //private Long strategyId;
    private Integer awardId;
    //private String awardKey;
    private String awardConfig;
    //private String awardDesc;
    private Integer sort;
    //奖品名称
    private String awardTitle;
}
