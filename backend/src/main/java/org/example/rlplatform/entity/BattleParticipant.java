package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "battle_participant")
public class BattleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(name = "student1_id", nullable = false)
    private Long student1Id;

    @Column(name = "student2_id", nullable = false)
    private Long student2Id;
}