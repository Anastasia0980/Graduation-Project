package org.example.rlplatform.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExperimentConfig {
    private String overview;
    private String rules;
    private String observationSpace;
    private String actionSpace;
    private String rewardFunction;
    private String evaluationFunction;

    /**
     * single 模式 baseline 选项：
     * - key: easy / medium / hard
     * - value: 该难度下允许选择的 baseline 列表（可按算法细分）
     */
    private Map<String, List<BaselineOption>> baselineOptions;
}
