package org.example.rlplatform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumProgressVO {

    /** 已通过的最高关卡序号 0…10（与 T1…T10 对应） */
    private int highestPassedTaskIndex;

    /** 下一次提交将评测的关卡 id，如 T3 */
    private String nextTaskId;
}
