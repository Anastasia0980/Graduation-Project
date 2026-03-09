package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.ModelFile;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.evaluation.EvaluationExecuter;
import org.example.rlplatform.service.EvaluationService;
import org.example.rlplatform.service.ModelFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationExecuter evaluationExecuter;

    @Autowired
    private ModelFileService modelFileService;

    @Value("${evaluation.workspace:}")
    private String workspace;

    @Override
    public void createEvaluation(Evaluation evaluation) {

        evaluation.setStatus("PENDING");
        evaluation.setCreateTime(LocalDateTime.now());
        evaluation.setUpdateTime(LocalDateTime.now());

        evaluationRepository.save(evaluation);
    }

    @Override
    public Evaluation getEvaluationById(Long id) {

        return evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("evaluation not found"));
    }

    @Override
    @Async
    public void runEvaluationAsync(Long id) {

        Evaluation evaluation = getEvaluationById(id);

        evaluation.setStatus("RUNNING");
        evaluation.setUpdateTime(LocalDateTime.now());

        evaluationRepository.save(evaluation);

        evaluationExecuter.execute(evaluation);

        evaluation.setUpdateTime(LocalDateTime.now());
        evaluationRepository.save(evaluation);
    }

    /**
     * 上传模型 + config 并立即运行评测
     */
    @Override
    public Result uploadAndRun(
            MultipartFile model,
            MultipartFile config,
            Long studentId,
            String agentName,
            String environment
    ) {

        try {

            // 1 上传模型
            ModelFile modelFile = modelFileService.uploadModelFile(model, studentId);

            // 2 保存config文件
            saveConfigFile(config);

            // 3 创建evaluation
            Evaluation evaluation = new Evaluation();

            evaluation.setStudentId(studentId);
            evaluation.setAgentName(agentName);
            evaluation.setEnvironment(environment);
            evaluation.setModelId(modelFile.getId());
            evaluation.setEpisodes(5);

            evaluation.setStatus("PENDING");
            evaluation.setCreateTime(LocalDateTime.now());
            evaluation.setUpdateTime(LocalDateTime.now());

            evaluationRepository.save(evaluation);

            // 4 运行评测
            runEvaluationAsync(evaluation.getId());

            return Result.success(evaluation.getId());

        } catch (Exception e) {

            return Result.error(e.getMessage());

        }
    }

    /**
     * 保存config文件
     */
    private void saveConfigFile(MultipartFile config) throws Exception {

        String baseDir = (workspace != null && !workspace.isBlank())
                ? workspace
                : Paths.get(System.getProperty("user.dir")).toString();

        Path configDir = Paths.get(baseDir, "configs");

        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }

        String fileName = UUID.randomUUID() + "_" + config.getOriginalFilename();

        Path path = configDir.resolve(fileName);

        config.transferTo(path);
    }
}