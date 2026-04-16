package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "battle_model_submission")
public class BattleModelSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Integer assignmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 128)
    private String environment;

    @Column(nullable = false)
    private Integer games;

    @Column(name = "student_dir_rel", nullable = false, length = 500)
    private String studentDirRel;

    @Column(name = "model_name", length = 255)
    private String modelName;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}