package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "team_group")
public class TeamGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "assignment_id", nullable = false)
    private Integer assignmentId;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Column(name = "team_code", nullable = false, length = 32, unique = true)
    private String teamCode;

    @Column(name = "captain_student_id", nullable = false)
    private Integer captainStudentId;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 3;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "locked", nullable = false)
    private Boolean locked = false;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
