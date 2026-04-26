package org.example.rlplatform.entity;

import lombok.Data;

@Data
public class BaselineOption {
    /**
     * 前端提交时使用的标识（建议唯一）
     * 例如：easy-dqn / hard-ddqn
     */
    private String id;

    /** 展示名称，例如 DQN / DDQN */
    private String label;

    /** 算法名（可与 label 相同），用于后端校验/记录 */
    private String algorithm;

    /**
     * baseline 模型文件路径（由平台控制）
     * 例如：baselines/tictactoe_v3/easy/dqn/baseline.pth
     */
    private String modelPath;
}

