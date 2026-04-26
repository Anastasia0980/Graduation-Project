package org.example.rlplatform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumProgressVO {

    /** 已通过关卡数：兼容 legacy（最高 T 序号）与 curriculumStages（已通关数） */
    private int highestPassedTaskIndex;

    /** 下一次评测对应的关卡 id（legacy 为 T?，新课程为 stageId） */
    private String nextTaskId;

    /** 总关卡数；legacy 固定为 10；无配置时为 0 */
    private int totalStages;

    /** DRAFT / PUBLISHED（null 视为 PUBLISHED） */
    private String publicationStatus;
}
