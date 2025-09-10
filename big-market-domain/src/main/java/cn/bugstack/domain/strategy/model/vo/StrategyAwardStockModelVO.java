package cn.bugstack.domain.strategy.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StrategyAwardStockModelVO {
    private Long strategyId;
    private Integer awardId;
}
