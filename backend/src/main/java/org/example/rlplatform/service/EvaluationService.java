package org.example.rlplatform.service;

import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.Result;
import org.springframework.web.multipart.MultipartFile;

public interface EvaluationService {

    void createEvaluation(Evaluation evaluation);

    Evaluation getEvaluationById(Long id);

    void runEvaluationAsync(Long id);

    /**
     * 上传模型并立即运行评测
     */
    Result uploadAndRun(
            MultipartFile model,
            MultipartFile config,
            Long studentId,
            String agentName,
            String environment
    );
}