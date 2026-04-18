package org.example.rlplatform.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "baseline")
public class Baseline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String environment;

    /** 与闯关 stageId 一致，最长 64（见 ExperimentAssignmentImpl 校验） */
    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(nullable = false)
    private String algorithm;

    // 相对 baselineRoot 的路径（例如：LunarLander-v3/T1/dqn/baseline.pth）
    @Column(name = "model_path", columnDefinition = "TEXT")
    private String modelPath;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

