package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生在某作业下的 Lunar 闯关进度：已通过的最高关卡编号（1=T1 … 10=T10），0 表示尚未通过任一关。
 */
@Data
@Entity
@Table(
        name = "assignment_curriculum_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "assignment_id"})
)
public class AssignmentCurriculumProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "assignment_id", nullable = false)
    private Integer assignmentId;

    /**
     * 已通过的最高任务序号：0 表示未通过 T1；10 表示已通过 T10（可重复挑战 T10）。
     */
    @Column(name = "highest_passed_task_index", nullable = false)
    private Integer highestPassedTaskIndex = 0;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
