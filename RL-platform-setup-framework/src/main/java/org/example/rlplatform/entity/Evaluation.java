package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="evaluation")
public class Evaluation {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "agent_name", nullable = false)
    private String agentName;

    @Column(nullable = false)
    private String environment;

    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "assignment_id", nullable = false)
    private Integer assignmentId;

    @Column(nullable = false)
    private Integer episodes;

    @Column(name = "baseline_difficulty")
    private String baselineDifficulty;

    @Column(name = "baseline_id")
    private String baselineId;

    @Column(name = "baseline_model_path", columnDefinition = "TEXT")
    private String baselineModelPath;

    /** 闯关关卡，如 T1…T10，传给评测脚本 --task_id */
    @Column(name = "task_id", length = 16)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationStatus status = EvaluationStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "Text")
    private String errorMessage;

    @Column(name="create_time")
    private LocalDateTime createTime;

    @Column(name="update_time")
    private LocalDateTime updateTime;
}