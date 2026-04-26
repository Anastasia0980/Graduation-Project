package org.example.rlplatform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.entity.CurriculumStageConfig;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.ExperimentConfig;
import org.example.rlplatform.entity.BaselineOption;
import org.example.rlplatform.entity.EvaluationMode;
import org.example.rlplatform.entity.PublicationStatus;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ExperimentAssignmentImpl implements ExperimentAssignmentService {
    private static final Pattern TASK_ID_LEGACY_PATTERN = Pattern.compile("^T(10|[1-9])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STAGE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    private static final Set<String> ALLOWED_ENV_SPEC_KEYS = Set.of(
            "enable_wind", "wind_power", "turbulence_power", "height_scale", "impulse_scale", "initial_angle_deg"
    );

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
    public Integer create(Integer classId, ExperimentAssignment experimentAssignment) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        experimentAssignment.setTeacherId(userId);
        experimentAssignment.setStudentClass(studentClassService.findByIdAndIsDeletedFalse(classId));
        experimentAssignment.setCreateTime(LocalDateTime.now());
        experimentAssignment.setUpdateTime(LocalDateTime.now());
        experimentAssignment.setIsDeleted(false);
        experimentAssignment.setPublicationStatus(PublicationStatus.DRAFT);

        ExperimentConfig config = experimentAssignment.getConfig();
        if (config != null) {
            validateConfigByMode(experimentAssignment.getEvaluationMode(), config, experimentAssignment.getEnvironment());
            try {
                experimentAssignment.setConfigJson(objectMapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("实验配置序列化失败", e);
            }
        }

        experimentAssignmentRepository.save(experimentAssignment);
        return experimentAssignment.getId();
    }

    @Override
    public void publish(Integer assignmentId) {
        ExperimentAssignment db = getById(assignmentId);
        if (db == null) {
            throw new RuntimeException("实验不存在");
        }
        if (db.getEffectivePublicationStatus() == PublicationStatus.PUBLISHED) {
            throw new IllegalStateException("任务已发布");
        }
        ExperimentConfig cfg = db.getConfig();
        if (cfg == null) {
            if (db.getEvaluationMode() == EvaluationMode.SINGLE) {
                throw new IllegalStateException("任务缺少配置，无法发布");
            }
        } else {
            validateConfigByMode(db.getEvaluationMode(), cfg, db.getEnvironment());
        }
        db.setPublicationStatus(PublicationStatus.PUBLISHED);
        db.setUpdateTime(LocalDateTime.now());
        experimentAssignmentRepository.save(db);
    }

    @Override
    public ExperimentAssignment update(Integer assignmentId, ExperimentAssignment experimentAssignment) {
        ExperimentAssignment dbassignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        if (dbassignment == null) {
            throw new RuntimeException("实验不存在");
        }

        ExperimentConfig incomingConfig = experimentAssignment.getConfig();
        if (incomingConfig != null && dbassignment.getEffectivePublicationStatus() == PublicationStatus.PUBLISHED && dbassignment.getEvaluationMode() == EvaluationMode.SINGLE) {
            try {
                JsonNode oldRoot = objectMapper.readTree(
                        dbassignment.getConfigJson() != null && !dbassignment.getConfigJson().isBlank()
                                ? dbassignment.getConfigJson() : "{}");
                JsonNode newRoot = objectMapper.valueToTree(incomingConfig);
                JsonNode oldStruct = curriculumStructureOnlyForPublishCompare(oldRoot);
                JsonNode newStruct = curriculumStructureOnlyForPublishCompare(newRoot);
                if (!oldStruct.equals(newStruct)) {
                    throw new IllegalArgumentException(
                            "已发布任务不允许修改闯关结构（关卡顺序、stageId、标题、envSpec、legacy 关卡键）；"
                                    + "可仅更新各关 baseline 引用或上传新 baseline 后保存");
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("已发布任务配置对比失败: " + e.getMessage());
            }
        }

        dbassignment.setTitle(experimentAssignment.getTitle());
        dbassignment.setConfig(incomingConfig);
        dbassignment.setEvaluationMode(experimentAssignment.getEvaluationMode());
        dbassignment.setAgentName(experimentAssignment.getAgentName());
        dbassignment.setEnvironment(experimentAssignment.getEnvironment());
        dbassignment.setTaskIcon(experimentAssignment.getTaskIcon());
        dbassignment.setDeadline(experimentAssignment.getDeadline());
        dbassignment.setTeamGroupDeadline(experimentAssignment.getTeamGroupDeadline());
        dbassignment.setUpdateTime(LocalDateTime.now());
        dbassignment.setIsDeleted(false);

        ExperimentConfig config = dbassignment.getConfig();
        if (config != null) {
            String envForValidation = experimentAssignment.getEnvironment() != null
                    ? experimentAssignment.getEnvironment()
                    : dbassignment.getEnvironment();
            validateConfigByMode(dbassignment.getEvaluationMode(), config, envForValidation);
            try {
                dbassignment.setConfigJson(objectMapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("实验配置序列化失败", e);
            }
        }

        return experimentAssignmentRepository.save(dbassignment);
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
        return experimentAssignmentRepository.findPublishedForStudentClass(
                sc.getId(), PublicationStatus.PUBLISHED, PageRequest.of(pageNum, pageSize));
    }

    @Override
    public Page<ExperimentAssignment> listTeaAssignments(Integer pageNum, Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        userService.findByIdAndIsDeletedFalse(userId);
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

    private static boolean usesCurriculumStages(ExperimentConfig config) {
        return config.getCurriculumStages() != null && !config.getCurriculumStages().isEmpty();
    }

    private void validateConfigByMode(EvaluationMode mode, ExperimentConfig config, String environment) {
        if (mode != EvaluationMode.SINGLE) {
            return;
        }
        if (config == null) {
            throw new IllegalArgumentException("SINGLE 作业必须包含 config");
        }
        if (config.getBaselineOptions() != null && !config.getBaselineOptions().isEmpty()) {
            throw new IllegalArgumentException("SINGLE 作业不再支持 baselineOptions(easy/medium/hard)，请使用 curriculumStages 或 taskBaselineOptions");
        }
        if (baselineRoot == null || baselineRoot.isBlank()) {
            throw new IllegalStateException("evaluation.baselineRoot 未配置，无法校验 baseline 文件是否存在");
        }

        if (usesCurriculumStages(config)) {
            validateCurriculumStages(config, isLunarEnvironment(environment));
            return;
        }

        Map<String, BaselineOption> taskBaselineOptions = config.getTaskBaselineOptions();
        if (taskBaselineOptions == null || taskBaselineOptions.isEmpty()) {
            throw new IllegalArgumentException("SINGLE 作业请配置 curriculumStages，或（兼容）完整 taskBaselineOptions（T1..T10）");
        }

        for (Map.Entry<String, BaselineOption> entry : taskBaselineOptions.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim().toUpperCase(Locale.ROOT);
            if (!TASK_ID_LEGACY_PATTERN.matcher(key).matches()) {
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
                throw new IllegalArgumentException("SINGLE 作业（兼容模式）必须完整配置 T1..T10，缺少: " + requiredTaskId);
            }
            BaselineOption option = taskBaselineOptions.get(requiredTaskId);
            if (option == null) {
                throw new IllegalArgumentException("关卡 " + requiredTaskId + " 未配置 baseline");
            }
        }
    }

    private void validateCurriculumStages(ExperimentConfig config, boolean lunarEnvironment) {
        List<CurriculumStageConfig> stages = config.getCurriculumStages();
        if (stages == null || stages.isEmpty()) {
            throw new IllegalArgumentException("curriculumStages 不能为空");
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < stages.size(); i++) {
            CurriculumStageConfig s = stages.get(i);
            if (s == null) {
                throw new IllegalArgumentException("curriculumStages[" + i + "] 不能为空");
            }
            String sid = s.getStageId() == null ? "" : s.getStageId().trim();
            if (!STAGE_ID_PATTERN.matcher(sid).matches()) {
                throw new IllegalArgumentException("非法 stageId: " + s.getStageId() + "（1-64 位字母数字下划线连字符）");
            }
            if (!seen.add(sid)) {
                throw new IllegalArgumentException("stageId 重复: " + sid);
            }
            JsonNode spec = s.getEnvSpec();
            if (lunarEnvironment) {
                if (spec == null || spec.isNull() || !spec.isObject() || spec.size() == 0) {
                    throw new IllegalArgumentException("LunarLander 关卡 " + sid + " 的 envSpec 必须为非空 JSON 对象");
                }
                var fieldNames = spec.fieldNames();
                while (fieldNames.hasNext()) {
                    String fn = fieldNames.next();
                    if (!ALLOWED_ENV_SPEC_KEYS.contains(fn)) {
                        throw new IllegalArgumentException("关卡 " + sid + " envSpec 含非法字段: " + fn);
                    }
                }
            }
            BaselineOption baseline = s.getBaseline();
            if (baseline == null) {
                throw new IllegalArgumentException("关卡 " + sid + " 未配置 baseline");
            }
            String modelPath = baseline.getModelPath();
            if (modelPath == null || modelPath.trim().isBlank()) {
                throw new IllegalArgumentException("关卡 " + sid + " baseline.modelPath 不能为空");
            }
            if (!modelPathExistsUnderBaselineRoot(modelPath)) {
                throw new IllegalArgumentException("关卡 " + sid + " baseline 文件不存在（基于 baselineRoot 校验）");
            }
        }
    }

    /**
     * 已发布单人任务保存时：仅允许改 baseline 内容，闯关「结构」须与库中一致。
     * 对 curriculumStages 去掉每关 baseline；对 taskBaselineOptions 只保留键集合（值忽略）。
     */
    private JsonNode curriculumStructureOnlyForPublishCompare(JsonNode root) {
        ObjectNode out = objectMapper.createObjectNode();
        JsonNode cs = root.path("curriculumStages");
        if (cs.isArray()) {
            ArrayNode arr = objectMapper.createArrayNode();
            for (JsonNode s : cs) {
                if (s != null && s.isObject()) {
                    ObjectNode stageCopy = s.deepCopy();
                    stageCopy.remove("baseline");
                    arr.add(stageCopy);
                } else {
                    arr.add(s);
                }
            }
            out.set("curriculumStages", arr);
        } else {
            out.set("curriculumStages", cs);
        }
        JsonNode tbo = root.path("taskBaselineOptions");
        if (tbo.isObject()) {
            ObjectNode keysOnly = objectMapper.createObjectNode();
            tbo.fieldNames().forEachRemaining(fn -> keysOnly.putNull(fn));
            out.set("taskBaselineOptions", keysOnly);
        } else {
            out.set("taskBaselineOptions", tbo);
        }
        return out;
    }

    private static boolean isLunarEnvironment(String environment) {
        return environment != null && "LunarLander-v3".equalsIgnoreCase(environment.trim());
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
