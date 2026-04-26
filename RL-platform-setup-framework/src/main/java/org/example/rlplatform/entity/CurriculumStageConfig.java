package org.example.rlplatform.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 有序闯关关卡：稳定 stageId + 环境参数 JSON（A2）+ 本关唯一 baseline。
 */
@Data
public class CurriculumStageConfig {
    private String stageId;
    private String title;
    /** LunarLanderTask 构造参数子集，见 lunar_task_env.TASK_ENV_KWARGS 键名 */
    private JsonNode envSpec;
    private BaselineOption baseline;
}
