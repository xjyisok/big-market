package cn.bugstack.domain.award.model.aggregate;

import cn.bugstack.domain.award.model.entity.TaskEntity;
import cn.bugstack.domain.award.model.entity.UserAwardRecordEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAwardRecordAggregate {
    private TaskEntity taskEntity;
    private UserAwardRecordEntity userAwardRecordEntity;
}
