package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "team_member")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "team_id", nullable = false)
    private Integer teamId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "join_time", nullable = false)
    private LocalDateTime joinTime;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
