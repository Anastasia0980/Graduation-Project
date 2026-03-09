package org.example.rlplatform.battle;

import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.entity.Evaluation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BattleRunner {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private BattleExecuter battleExecuter;

    @Async
    public void runBattleAsync(Long evaluationId, String student1DirRel, String student2DirRel) {

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("evaluation not found"));

        try {
            evaluation.setStatus("RUNNING");
            evaluation.setUpdateTime(LocalDateTime.now());
            evaluationRepository.save(evaluation);

            battleExecuter.executeBattle(evaluation, student1DirRel, student2DirRel);

        } finally {
            evaluation.setUpdateTime(LocalDateTime.now());
            evaluationRepository.save(evaluation);
        }
    }
}