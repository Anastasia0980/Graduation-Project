package org.example.rlplatform.battle;

import org.example.rlplatform.Repository.BattleParticipantRepository;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.entity.BattleParticipant;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.EvaluationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class BattleRunner {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private BattleParticipantRepository battleParticipantRepository;

    @Autowired
    private BattleExecuter battleExecuter;

    @Async("evaluationExecutor")
    public void runBattleAsync(Long evaluationId) {
        log.info("Battle async start id={}", evaluationId);
        Optional<Evaluation> loaded = evaluationRepository.findById(evaluationId);
        if (loaded.isEmpty()) {
            log.error("Battle async aborted: evaluation not found id={}", evaluationId);
            throw new IllegalStateException("evaluation not found: " + evaluationId);
        }
        Evaluation evaluation = loaded.get();

        try {
            Optional<BattleParticipant> participantOpt = battleParticipantRepository.findByEvaluationId(evaluationId);
            if (participantOpt.isEmpty()) {
                log.warn("Battle async id={} aborted: battle participant not found", evaluationId);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("battle participant not found");
                evaluation.setUpdateTime(LocalDateTime.now());
                evaluationRepository.save(evaluation);
                return;
            }

            log.info("Battle async running id={} env={} episodes={}",
                    evaluationId, evaluation.getEnvironment(), evaluation.getEpisodes());

            evaluation.setStatus(EvaluationStatus.RUNNING);
            evaluation.setErrorMessage(null);
            evaluation.setUpdateTime(LocalDateTime.now());
            evaluationRepository.save(evaluation);

            battleExecuter.executeBattle(evaluation, participantOpt.get());

            evaluation.setUpdateTime(LocalDateTime.now());
            evaluationRepository.save(evaluation);
        } catch (Exception e) {
            log.error("Battle async failed after load id={}", evaluationId, e);
            throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
        }
    }
}