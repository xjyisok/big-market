package cn.bugstack.domain.strategy.model.vo;

import cn.bugstack.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StrategyAwardRuleModelVO {
    private String ruleModels;
    public String[] raffleCenterRuleModelList(){
        List<String>raffleCenterRuleModels = new ArrayList<String>();
        String[] raffleCenterRuleModelArray = ruleModels.split(",");
        for(String raffleCenterRuleModel : raffleCenterRuleModelArray){
            if(DefaultLogicFactory.LogicModel.isIn(raffleCenterRuleModel)){
                raffleCenterRuleModels.add(raffleCenterRuleModel);
            }
        }
        return raffleCenterRuleModels.toArray(new String[0]);
    }
    public String[] raffleAfterRuleModelList(){
        List<String>raffleAfterRuleModels = new ArrayList<String>();
        String[] raffleAfterRuleModelArray = ruleModels.split(",");
        for(String raffleAfterRuleModel : raffleAfterRuleModelArray){
            if(DefaultLogicFactory.LogicModel.isIn(raffleAfterRuleModel)){
                raffleAfterRuleModels.add(raffleAfterRuleModel);
            }
        }
        return raffleAfterRuleModels.toArray(new String[0]);
    }
}
