package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.entity.*;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.evaluation.EvaluationExecuter;
import org.example.rlplatform.evaluation.EvaluationRunner;
import org.example.rlplatform.service.CurriculumProgressService;
import org.example.rlplatform.service.ModelFileService;
import org.example.rlplatform.service.EvaluationService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;
import org.springframework.web.multipart.MultipartFile;

import static java.time.LocalDateTime.now;

@Slf4j
@Service
public class EvaluationServiceImpl implements EvaluationService {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationExecuter evaluationExecuter;

    @Autowired
    private ModelFileService modelFileService;

    @Autowired
    private ExperimentAssignmentRepository experimentAssignmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EvaluationRunner evaluationRunner;

    @Autowired
    private CurriculumProgressService curriculumProgressService;

    @Value("${evaluation.baselineRoot:}")
    private String baselineRoot;

    @Override
    public void createEvaluation(Evaluation evaluation) {
        evaluation.setCreateTime(now());
        evaluation.setUpdateTime(now());
        evaluationRepository.save(evaluation);
    }

    @Override
    public Evaluation getEvaluationById(long id) {
        return evaluationRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @Override
    public void runEvaluationByConfig(Integer assignmentId, MultipartFile model, MultipartFile config) {
        Evaluation evaluation = null;
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims == null || claims.get("id") == null) {
                throw new IllegalStateException("当前登录信息无效，请重新登录");
            }

            Integer currentUserId = (Integer) claims.get("id");
            Integer studentId = currentUserId;

            ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("任务不存在");
            }

            if (assignment.getEvaluationMode() != EvaluationMode.SINGLE) {
                throw new IllegalStateException("当前任务不是单人模式，不能进行单人评测");
            }

            String environment = assignment.getEnvironment();
            if (environment == null || environment.isBlank()) {
                throw new IllegalStateException("任务未配置环境");
            }

            int episodes = 10;

            ModelFile modelFile = modelFileService.uploadModelWithConfig(model, config, studentId);

            String taskId = curriculumProgressService.resolveNextTaskId(studentId, assignmentId);

            // 从任务配置中解析 baselineOptions
            ExperimentConfig taskConfig = null;
            if (assignment.getConfigJson() != null && !assignment.getConfigJson().isBlank()) {
                try {
                    taskConfig = objectMapper.readValue(assignment.getConfigJson(), ExperimentConfig.class);
                } catch (Exception ignored) {
                    taskConfig = null;
                }
            }
            BaselineOption chosen = selectBaselineForTask(taskConfig, taskId);

            if (chosen.getModelPath() == null || chosen.getModelPath().isBlank()) {
                throw new IllegalStateException("baseline 选项未配置 modelPath");
            }

            String normalizedModelPath = chosen.getModelPath().replace("\\", "/");
            String baselineModelPath = (baselineRoot != null && !baselineRoot.isBlank())
                    ? baselineRoot.replace("\\", "/").replaceAll("/+$", "") + "/" + normalizedModelPath.replaceAll("^/+", "")
                    : normalizedModelPath;

            evaluation = new Evaluation();
            evaluation.setStudentId(studentId);
            evaluation.setAgentName(assignment.getAgentName());
            evaluation.setEnvironment(environment);
            evaluation.setModelId(modelFile.getId());
            evaluation.setAssignmentId(assignmentId);
            evaluation.setEpisodes(episodes);
            // 闯关模式下不再使用 baselineDifficulty（历史字段，避免写入错误语义）
            evaluation.setBaselineDifficulty(null);
            evaluation.setBaselineId(chosen.getId());
            evaluation.setBaselineModelPath(baselineModelPath);
            evaluation.setTaskId(taskId);
            evaluation.setStatus(EvaluationStatus.PENDING);
            evaluation.setCreateTime(now());
            evaluation.setUpdateTime(now());
            evaluationRepository.save(evaluation);

            evaluationRunner.runAsync(evaluation.getId());

        } catch (Exception e) {
            log.error("runEvaluationByConfig failed assignmentId={} evaluationId={}",
                    assignmentId, evaluation != null ? evaluation.getId() : null, e);
            if (evaluation != null) {
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage(e.getMessage());
                evaluation.setUpdateTime(now());
                evaluationRepository.save(evaluation);
            }
            throw new RuntimeException(e);
        }
    }

    private BaselineOption selectBaselineForTask(ExperimentConfig taskConfig, String taskId) {
        String normalizedTaskId = taskId == null ? "" : taskId.trim().toUpperCase(Locale.ROOT);
        if (normalizedTaskId.isBlank()) {
            throw new IllegalStateException("无法确定当前闯关 taskId");
        }

        BaselineOption selected = (taskConfig != null && taskConfig.getTaskBaselineOptions() != null)
                ? taskConfig.getTaskBaselineOptions().get(normalizedTaskId)
                : null;
        if (selected != null) {
            return selected;
        }

        throw new IllegalStateException("任务未配置当前关卡 baseline（taskId=" + normalizedTaskId + "）");
    }

//    @Override
//    public void runEvaluationByBot(Long evaluationId) {
//        Evaluation evaluation = getEvaluationById(evaluationId);
//        evaluation.setErrorMessage(null);
//        evaluation.setStatus(EvaluationStatus.RUNNING);
//        evaluation.setUpdateTime(now());
//        evaluationRepository.save(evaluation);
//
//    }

    @Override
    public Page<Evaluation> list(Integer pageNum, Integer pageSize, Integer assignmentId, Integer studentId, String status) {
        Specification<Evaluation> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (assignmentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignmentId"), assignmentId));
            }
            if (studentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("studentId"), studentId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return evaluationRepository.findAll(spec, PageRequest.of(pageNum, pageSize));
    }

}
