package org.example.rlplatform.controller;

import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/evaluations")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    /**
     * 上传模型和config并立即运行评测
     */
    @PostMapping("/uploadAndRun")
    public Result uploadAndRun(
            @RequestParam("model") MultipartFile model,
            @RequestParam("config") MultipartFile config,
            @RequestParam("studentId") Long studentId,
            @RequestParam("agentName") String agentName,
            @RequestParam("environment") String environment
    ) {

        return evaluationService.uploadAndRun(
                model,
                config,
                studentId,
                agentName,
                environment
        );
    }

    @PostMapping
    public Result<Void> submitEvaluation(@RequestBody Evaluation evaluation) {
        evaluationService.createEvaluation(evaluation);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Evaluation> getEvaluationById(@PathVariable long id) {
        return Result.success(evaluationService.getEvaluationById(id));
    }

    @PostMapping("/{id}/run")
    public Result<Void> runEvaluation(@PathVariable long id) {
        evaluationService.runEvaluationAsync(id);
        return Result.success();
    }
}