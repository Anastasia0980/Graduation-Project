package org.example.rlplatform.controller;

import org.example.rlplatform.entity.EvaluationResult;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.EvaluationResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/evaluation-results")
public class EvaluationResultController {

    @Autowired
    private EvaluationResultService evaluationResultService;

    @PostMapping
    public Result<Void> create(@RequestBody EvaluationResult evaluationResult) {
        evaluationResultService.create(evaluationResult);
        return Result.success();
    }

    @GetMapping("/id/{id}")
    public Result<EvaluationResult> getById(@PathVariable Long id) {
        return Result.success(evaluationResultService.getById(id));
    }

    @GetMapping
    public Result<List<EvaluationResult>> list(@RequestParam(required = false) Long evaluationId) {
        if (evaluationId != null) {
            return Result.success(evaluationResultService.listByEvaluationId(evaluationId));
        }
        return Result.success(evaluationResultService.list());
    }

    @GetMapping(value = "/{id}/video", produces = "video/mp4")
    public ResponseEntity<Resource> getVideo(@PathVariable Long id) {
        Resource resource = evaluationResultService.getVideo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }

    @GetMapping("/evaluation/{evaluationId}")
    public Result<List<EvaluationResult>> getByEvaluationId(@PathVariable Long evaluationId) {
        return Result.success(evaluationResultService.listByEvaluationId(evaluationId));
    }

    @GetMapping(value = "/{id}/log", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<Resource> downloadLog(@PathVariable Long id) {
        Resource resource = evaluationResultService.getLog(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"evaluation_" + id + "_log.txt\"")
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(resource);
    }

    @GetMapping(value = "/evaluation/{evaluationId}/model-package", produces = "application/zip")
    public ResponseEntity<Resource> downloadModelPackage(@PathVariable Long evaluationId) {
        Resource resource = evaluationResultService.downloadModelPackage(evaluationId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"evaluation_" + evaluationId + "_model.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(resource);
    }
}