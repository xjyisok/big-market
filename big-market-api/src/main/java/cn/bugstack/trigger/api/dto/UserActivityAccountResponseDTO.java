package cn.bugstack.trigger.api.dto;

import lombok.Data;

@Data
public class UserActivityAccountResponseDTO {
    private Integer totalCount;
    private Integer totalCountSurplus;
    private Integer dayCount;
    private Integer dayCountSurplus;
    private Integer monthCount;
    private Integer monthCountSurplus;
}
