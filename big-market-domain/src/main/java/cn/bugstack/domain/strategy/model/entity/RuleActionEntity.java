package cn.bugstack.domain.strategy.model.entity;

import cn.bugstack.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleActionEntity<T extends RuleActionEntity.RaffleEntity> {
    private String code= RuleLogicCheckTypeVO.ALLOW.getCode();
    private String info= RuleLogicCheckTypeVO.ALLOW.getInfo();
    private String ruleModel;
    private T data;
    static public class RaffleEntity {

    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper=true)
    static public class RaffleBeforeEntity extends RaffleEntity {
        private Long strategyId;
        private String ruleWeightValueKey;
        private Integer awardId;
    }
    static public class RaffleInEntity extends RaffleEntity {

    }
    static public class RaffleAfterEntity extends RaffleEntity {

    }
}
