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

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false)
    private String algorithm;

    // 相对 baselineRoot 的路径（例如：tictactoe_v3/easy/dqn/baseline.pth）
    @Column(name = "model_path", columnDefinition = "TEXT")
    private String modelPath;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

