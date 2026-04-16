package org.example.rlplatform.service;

import org.example.rlplatform.vo.CurriculumProgressVO;

public interface CurriculumProgressService {

    /**
     * 根据进度返回本次提交应使用的 task_id（T1…T10）。
     */
    String resolveNextTaskId(Integer studentId, Integer assignmentId);

    /** 在脚本已成功结束且超过 baseline 后调用，推进闯关进度。 */
    void recordPassedStage(Integer studentId, Integer assignmentId, String taskIdNormalized);

    CurriculumProgressVO getProgress(Integer studentId, Integer assignmentId);
}
