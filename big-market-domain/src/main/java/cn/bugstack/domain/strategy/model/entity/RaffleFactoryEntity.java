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
public class RaffleFactoryEntity {
    String userId;
    Long strategyId;
    Integer awardId;
    /**
     * 结束时间
     */
    private Date endDateTime;
}
