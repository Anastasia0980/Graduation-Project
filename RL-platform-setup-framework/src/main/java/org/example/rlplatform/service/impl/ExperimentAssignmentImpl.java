package org.example.rlplatform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.ExperimentConfig;
import org.example.rlplatform.entity.BaselineOption;
import org.example.rlplatform.entity.EvaluationMode;
import org.example.rlplatform.entity.StudentClass;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.service.ExperimentAssignmentService;
import org.example.rlplatform.service.StudentClassService;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ExperimentAssignmentImpl implements ExperimentAssignmentService {
    private static final Pattern TASK_ID_PATTERN = Pattern.compile("^T(10|[1-9])$");

    @Autowired
    private StudentClassService studentClassService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExperimentAssignmentRepository experimentAssignmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${evaluation.baselineRoot:}")
    private String baselineRoot;

    @Override
    public void create(Integer classId, ExperimentAssignment experimentAssignment) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        experimentAssignment.setTeacherId(userId);
        experimentAssignment.setStudentClass(studentClassService.findByIdAndIsDeletedFalse(classId));
        experimentAssignment.setCreateTime(LocalDateTime.now());
        experimentAssignment.setUpdateTime(LocalDateTime.now());
        experimentAssignment.setIsDeleted(false);

        ExperimentConfig config = experimentAssignment.getConfig();
        if (config != null) {
            validateConfigByMode(experimentAssignment.getEvaluationMode(), config);
            try {
                experimentAssignment.setConfigJson(objectMapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("实验配置序列化失败", e);
            }
        }

        experimentAssignmentRepository.save(experimentAssignment);
    }

    @Override
    public void update(Integer assignmentId, ExperimentAssignment experimentAssignment) {
        ExperimentAssignment dbassignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        System.out.println(dbassignment);
        if (dbassignment == null) {
            throw new RuntimeException("实验不存在");
        }
        dbassignment.setTitle(experimentAssignment.getTitle());
        dbassignment.setConfig(experimentAssignment.getConfig());
        dbassignment.setEvaluationMode(experimentAssignment.getEvaluationMode());
        dbassignment.setAgentName(experimentAssignment.getAgentName());
        dbassignment.setEnvironment(experimentAssignment.getEnvironment());
        dbassignment.setDeadline(experimentAssignment.getDeadline());
        dbassignment.setTeamGroupDeadline(experimentAssignment.getTeamGroupDeadline());
        dbassignment.setUpdateTime(LocalDateTime.now());
        dbassignment.setIsDeleted(false);

        ExperimentConfig config = dbassignment.getConfig();
        if (config != null) {
            validateConfigByMode(dbassignment.getEvaluationMode(), config);
            try {
                dbassignment.setConfigJson(objectMapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("实验配置序列化失败", e);
            }
        }

        experimentAssignmentRepository.save(dbassignment);
    }

    @Override
    public Page<ExperimentAssignment> listStuAssignments(Integer pageNum, Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        User me = userService.findByIdAndIsDeletedFalse(userId);
        StudentClass sc = me.getStudentClass();
        if (sc == null) {
            throw new RuntimeException("您还未选择班级");
        }
        if (sc.getIsDeleted()) {
            throw new RuntimeException("班级已删除");
        }
        return experimentAssignmentRepository.findByStudentClass_IdAndIsDeletedFalse(sc.getId(), PageRequest.of(pageNum, pageSize));
    }

    @Override
    public Page<ExperimentAssignment> listTeaAssignments(Integer pageNum, Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        User me = userService.findByIdAndIsDeletedFalse(userId);
        return experimentAssignmentRepository.findByTeacherIdAndIsDeletedFalse(userId, PageRequest.of(pageNum, pageSize));
    }

    @Override
    public Page<ExperimentAssignment> listAssignmentsByClass(Integer classId, Integer pageNum, Integer pageSize) {
        return experimentAssignmentRepository.findByStudentClass_IdAndIsDeletedFalse(classId, PageRequest.of(pageNum, pageSize));
    }

    @Override
    public ExperimentAssignment getById(Integer assignmentId) {
        ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        if (assignment == null) {
            return null;
        }
        String configJson = assignment.getConfigJson();
        if (configJson != null && !configJson.isBlank()) {
            try {
                ExperimentConfig config = objectMapper.readValue(configJson, ExperimentConfig.class);
                assignment.setConfig(config);
            } catch (JsonProcessingException e) {
                // 配置解析失败时暂时忽略，保留原始 JSON
            }
        }
        return assignment;
    }

    @Override
    public void softDelete(Integer assignmentId) {
        ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        if (assignment == null) {
            throw new RuntimeException("实验不存在或已删除");
        }
        assignment.setIsDeleted(true);
        assignment.setUpdateTime(LocalDateTime.now());
        experimentAssignmentRepository.save(assignment);
    }

    private void validateConfigByMode(EvaluationMode mode, ExperimentConfig config) {
        if (mode != EvaluationMode.SINGLE) {
            return;
        }
        if (config == null) {
            throw new IllegalArgumentException("SINGLE 作业必须配置 taskBaselineOptions");
        }
        Map<String, BaselineOption> taskBaselineOptions = config.getTaskBaselineOptions();
        if (taskBaselineOptions == null || taskBaselineOptions.isEmpty()) {
            throw new IllegalArgumentException("SINGLE 作业必须配置 taskBaselineOptions（key 仅允许 T1..T10）");
        }
        if (config.getBaselineOptions() != null && !config.getBaselineOptions().isEmpty()) {
            throw new IllegalArgumentException("SINGLE 作业不再支持 baselineOptions(easy/medium/hard)，请改为 taskBaselineOptions");
        }
        if (baselineRoot == null || baselineRoot.isBlank()) {
            throw new IllegalStateException("evaluation.baselineRoot 未配置，无法校验 baseline 文件是否存在");
        }

        for (Map.Entry<String, BaselineOption> entry : taskBaselineOptions.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim().toUpperCase();
            if (!TASK_ID_PATTERN.matcher(key).matches()) {
                throw new IllegalArgumentException("taskBaselineOptions 包含非法关卡 key: " + entry.getKey() + "（仅允许 T1..T10）");
            }

            BaselineOption option = entry.getValue();
            if (option == null) {
                throw new IllegalArgumentException("关卡 " + key + " 未配置 baseline");
            }
            String modelPath = option.getModelPath();
            if (modelPath == null || modelPath.trim().isBlank()) {
                throw new IllegalArgumentException("关卡 " + key + " 缺少可用 modelPath");
            }
            if (!modelPathExistsUnderBaselineRoot(modelPath)) {
                throw new IllegalArgumentException("关卡 " + key + " 的 modelPath 文件不存在（基于 baselineRoot 校验）");
            }
        }

        for (int i = 1; i <= 10; i++) {
            String requiredTaskId = "T" + i;
            if (!taskBaselineOptions.containsKey(requiredTaskId)) {
                throw new IllegalArgumentException("SINGLE 作业必须完整配置 T1..T10，缺少: " + requiredTaskId);
            }
            BaselineOption option = taskBaselineOptions.get(requiredTaskId);
            if (option == null) {
                throw new IllegalArgumentException("关卡 " + requiredTaskId + " 未配置 baseline");
            }
        }
    }

    private boolean modelPathExistsUnderBaselineRoot(String modelPath) {
        if (modelPath == null || modelPath.trim().isBlank()) {
            return false;
        }
        String normalized = modelPath.trim().replace("\\", "/");
        Path candidate;
        if (Paths.get(normalized).isAbsolute()) {
            candidate = Paths.get(normalized);
        } else {
            String root = baselineRoot.trim().replace("\\", "/").replaceAll("/+$", "");
            candidate = Paths.get(root, normalized.replaceAll("^/+", ""));
        }
        return Files.exists(candidate);
    }
}
