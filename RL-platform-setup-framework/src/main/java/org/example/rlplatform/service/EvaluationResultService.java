package org.example.rlplatform.service;

import org.example.rlplatform.entity.EvaluationResult;
import org.springframework.core.io.Resource;

import java.util.List;

public interface EvaluationResultService {

    void create(EvaluationResult evaluationResult);

    EvaluationResult getById(Long id);

    List<EvaluationResult> listByEvaluationId(Long evaluationId);

    List<EvaluationResult> list();

    Resource getVideo(Long id);

    Resource getLog(Long id);

    Resource downloadModelPackage(Long evaluationId);
}