package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "score_export_record")
public class ScoreExportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Integer teacherId;

    @Column(name = "assignment_id", nullable = false)
    private Integer assignmentId;

    @Column(name = "class_name", length = 255)
    private String className;

    @Column(name = "task_name", length = 255)
    private String taskName;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "export_time")
    private LocalDateTime exportTime;
}