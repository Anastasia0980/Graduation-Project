package org.example.rlplatform.evaluation;

import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.EvaluationStatus;
import org.example.rlplatform.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static java.time.LocalDateTime.now;

/**
 * 异步执行评测：与 {@link EvaluationService#runEvaluation(Long)} 解耦，避免 Service 自调用导致 @Async 失效，
 * 同时避免在 Service 实现类上注入自身接口带来的循环依赖（此处对 EvaluationService 使用 @Lazy）。
 */
@Slf4j
@Component
public class EvaluationRunner {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationExecuter evaluationExecuter;

    @Async("evaluationExecutor")
    public void runAsync(long evaluationId) {
        log.info("Evaluation async start id={}", evaluationId);
        Optional<Evaluation> loaded = evaluationRepository.findById(evaluationId);
        if (loaded.isEmpty()) {
            log.error("Evaluation async aborted: record not found id={}", evaluationId);
            throw new IllegalStateException("Evaluation not found: " + evaluationId);
        }
        Evaluation evaluation = loaded.get();

        try {
            // if (evaluation.getStatus() == EvaluationStatus.RUNNING) {
            //     throw new RuntimeException("Evaluation is already running");
            // } else if (evaluation.getStatus() == EvaluationStatus.FINISHED) {
            //     throw new RuntimeException("Evaluation is already finished");
            // }
            evaluation.setErrorMessage(null);
            evaluation.setStatus(EvaluationStatus.RUNNING);
            evaluation.setUpdateTime(now());
            evaluationRepository.save(evaluation);

            evaluationExecuter.execute(evaluation);

            evaluation.setUpdateTime(now());
            evaluationRepository.save(evaluation);
        } catch (Exception e) {
            log.error("Evaluation async failed after load id={}", evaluationId, e);
            throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
        }
    }
}
