package cn.bugstack.trigger.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserActivityAccountRequestDTO implements Serializable {
    String userId;
    Long activityId;
}
