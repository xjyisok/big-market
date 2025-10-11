package cn.bugstack.trigger.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleAwardListResponseDTO {
    private Integer awardId;
    private String awardTitle;
    private String awardSubtitle;
    private Integer sort;
    //奖品次数规则抽奖n次后解锁
    private Integer awardRuleLockCount;
    //是否解锁
    private boolean isAwardUnlock;
    //仍然需要的解锁次数
    private Integer waitUnlockCount;
}
