package org.example.rlplatform.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExperimentConfig {
    private List<String> algorithmOptions;
    private String overview;
    private String rules;
    private String observationSpace;
    private String actionSpace;
    private String rewardFunction;
    private String evaluationFunction;
    private Integer teamMaxMembers;

    /**
     * single 模式 baseline 选项：
     * - key: easy / medium / hard
     * - value: 该难度下允许选择的 baseline 列表（可按算法细分）
     */
    private Map<String, List<BaselineOption>> baselineOptions;

    /**
     * 闯关模式 baseline 选项：
     * - key: T1 ... T10
     * - value: 当前关唯一 baseline
     */
    private Map<String, BaselineOption> taskBaselineOptions;
}
