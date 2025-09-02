package cn.bugstack.infrastructure.persistent.po;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class StrategyAward {
    private Long id;
    private Long strategyId;
    private Integer awardId;
    private String awardTitle;
    private String awardSubTitle;
    private Integer awardCount;
    private Integer awardCountSurplus;
    private BigDecimal awardRate;
    private Integer sort;
    private Date createTime;
    private Date updateTime;
    private String ruleModels;

}
