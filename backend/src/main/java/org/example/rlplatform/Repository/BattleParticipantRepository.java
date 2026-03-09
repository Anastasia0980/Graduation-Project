package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.BattleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, Long> {

    Optional<BattleParticipant> findByEvaluationId(Long evaluationId);
}