package cn.bugstack.trigger.api.dto;

import lombok.Data;

@Data
public class UserActivityAccountRequestDTO {
    String userId;
    Long activityId;
}
