package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByAgentNameOrderByIdDesc(String agentName);
}