package org.example.rlplatform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.example.rlplatform.Repository.ClassGroupPlanMemberRepository;
import org.example.rlplatform.Repository.ClassGroupPlanRepository;
import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.Repository.TeamGroupRepository;
import org.example.rlplatform.Repository.TeamMemberRepository;
import org.example.rlplatform.entity.CurriculumStageConfig;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.ExperimentConfig;
import org.example.rlplatform.entity.BaselineOption;
import org.example.rlplatform.entity.ClassGroupPlan;
import org.example.rlplatform.entity.ClassGroupPlanMember;
import org.example.rlplatform.entity.EvaluationMode;
import org.example.rlplatform.entity.PublicationStatus;
import org.example.rlplatform.entity.StudentClass;
import org.example.rlplatform.entity.TeamGroup;
import org.example.rlplatform.entity.TeamMember;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private ClassGroupPlanRepository classGroupPlanRepository;
    @Autowired
    private ClassGroupPlanMemberRepository classGroupPlanMemberRepository;
    @Autowired
    private TeamGroupRepository teamGroupRepository;
    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${evaluation.baselineRoot:}")
    private String baselineRoot;

    @Override
    @Transactional
    public Integer create(Integer classId, ExperimentAssignment experimentAssignment) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        StudentClass studentClass = studentClassService.findByIdAndIsDeletedFalse(classId);
        experimentAssignment.setTeacherId(userId);
        experimentAssignment.setStudentClass(studentClass);
        experimentAssignment.setCreateTime(LocalDateTime.now());
        experimentAssignment.setUpdateTime(LocalDateTime.now());
        experimentAssignment.setIsDeleted(false);
        experimentAssignment.setPublicationStatus(PublicationStatus.DRAFT);
        boolean useSavedGroup = experimentAssignment.getEvaluationMode() == EvaluationMode.TEAM
                && Boolean.TRUE.equals(experimentAssignment.getUseSavedGroup());
        experimentAssignment.setUseSavedGroup(useSavedGroup);
        if (!useSavedGroup) {
            experimentAssignment.setGroupPlanId(null);
        } else {
            validateSavedGroupPlan(studentClass.getId(), experimentAssignment.getGroupPlanId());
            experimentAssignment.setTeamGroupDeadline(null);
        }

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
        if (useSavedGroup) {
            createTeamsFromSavedGroupPlan(experimentAssignment);
        }
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
    @Transactional
    public ExperimentAssignment update(Integer assignmentId, ExperimentAssignment experimentAssignment) {
        ExperimentAssignment dbassignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        if (dbassignment == null) {
            throw new RuntimeException("实验不存在");
        }
        boolean previousUseSavedGroup = Boolean.TRUE.equals(dbassignment.getUseSavedGroup());
        Integer previousGroupPlanId = dbassignment.getGroupPlanId();

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
        boolean useSavedGroup = dbassignment.getEvaluationMode() == EvaluationMode.TEAM
                && Boolean.TRUE.equals(experimentAssignment.getUseSavedGroup());
        dbassignment.setUseSavedGroup(useSavedGroup);
        dbassignment.setGroupPlanId(useSavedGroup ? experimentAssignment.getGroupPlanId() : null);
        if (useSavedGroup) {
            validateSavedGroupPlan(
                    dbassignment.getStudentClass() == null ? null : dbassignment.getStudentClass().getId(),
                    dbassignment.getGroupPlanId()
            );
            dbassignment.setTeamGroupDeadline(null);
        }
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

        ExperimentAssignment saved = experimentAssignmentRepository.save(dbassignment);
        boolean groupPlanChanged = !Objects.equals(previousGroupPlanId, saved.getGroupPlanId());
        if (previousUseSavedGroup && !useSavedGroup) {
            clearTeamsForAssignment(saved.getId());
        } else if (useSavedGroup && (!previousUseSavedGroup || groupPlanChanged)) {
            clearTeamsForAssignment(saved.getId());
            createTeamsFromSavedGroupPlan(saved);
        }
        return saved;
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

    private ClassGroupPlan validateSavedGroupPlan(Integer classId, Integer groupPlanId) {
        if (groupPlanId == null) {
            throw new IllegalArgumentException("请选择分组方案");
        }
        ClassGroupPlan plan = classGroupPlanRepository.findByIdAndActiveTrue(groupPlanId);
        if (plan == null) {
            throw new IllegalArgumentException("分组方案不存在或已失效");
        }
        if (!Objects.equals(plan.getClassId(), classId)) {
            throw new IllegalArgumentException("分组方案不属于当前班级");
        }
        List<ClassGroupPlanMember> members = classGroupPlanMemberRepository.findByPlanIdOrderByGroupNoAscMemberOrderAsc(groupPlanId);
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("分组方案没有成员明细");
        }
        return plan;
    }

    private void clearTeamsForAssignment(Integer assignmentId) {
        List<TeamGroup> teams = teamGroupRepository.findByAssignmentIdAndIsDeletedFalseOrderByIdAsc(assignmentId);
        for (TeamGroup team : teams) {
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsDeletedFalseOrderByIdAsc(team.getId());
            for (TeamMember member : members) {
                member.setIsDeleted(true);
            }
            teamMemberRepository.saveAll(members);
            team.setIsDeleted(true);
            team.setUpdateTime(LocalDateTime.now());
        }
        teamGroupRepository.saveAll(teams);
    }

    private void createTeamsFromSavedGroupPlan(ExperimentAssignment assignment) {
        Integer classId = assignment.getStudentClass() == null ? null : assignment.getStudentClass().getId();
        ClassGroupPlan plan = validateSavedGroupPlan(classId, assignment.getGroupPlanId());
        List<ClassGroupPlanMember> members = classGroupPlanMemberRepository.findByPlanIdOrderByGroupNoAscMemberOrderAsc(plan.getId());
        Map<String, List<ClassGroupPlanMember>> grouped = members.stream()
                .collect(Collectors.groupingBy(
                        ClassGroupPlanMember::getGroupNo,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (Map.Entry<String, List<ClassGroupPlanMember>> entry : grouped.entrySet()) {
            List<ClassGroupPlanMember> groupMembers = entry.getValue();
            if (groupMembers == null || groupMembers.isEmpty()) {
                continue;
            }
            groupMembers = groupMembers.stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getMemberOrder() == null ? 0 : a.getMemberOrder(),
                            b.getMemberOrder() == null ? 0 : b.getMemberOrder()
                    ))
                    .toList();

            ClassGroupPlanMember captain = groupMembers.get(0);
            User captainUser = userService.findByIdAndIsDeletedFalse(captain.getStudentId());
            if (captainUser.getStudentClass() == null || !Objects.equals(captainUser.getStudentClass().getId(), classId)) {
                throw new IllegalArgumentException("分组方案中存在不属于当前班级的学生：" + captain.getStudentNoSnapshot());
            }

            TeamGroup team = new TeamGroup();
            team.setAssignmentId(assignment.getId());
            team.setTeamName(entry.getKey());
            team.setTeamCode(generateTeamCode());
            team.setCaptainStudentId(captain.getStudentId());
            team.setMaxMembers(3);
            team.setCreateTime(LocalDateTime.now());
            team.setUpdateTime(LocalDateTime.now());
            team.setLocked(true);
            team.setLockTime(LocalDateTime.now());
            team.setIsDeleted(false);
            team = teamGroupRepository.save(team);

            int limit = Math.min(3, groupMembers.size());
            for (int i = 0; i < limit; i++) {
                ClassGroupPlanMember planMember = groupMembers.get(i);
                User memberUser = userService.findByIdAndIsDeletedFalse(planMember.getStudentId());
                if (memberUser.getStudentClass() == null || !Objects.equals(memberUser.getStudentClass().getId(), classId)) {
                    throw new IllegalArgumentException("分组方案中存在不属于当前班级的学生：" + planMember.getStudentNoSnapshot());
                }
                TeamMember member = new TeamMember();
                member.setTeamId(team.getId());
                member.setStudentId(planMember.getStudentId());
                member.setJoinTime(LocalDateTime.now());
                member.setIsDeleted(false);
                teamMemberRepository.save(member);
            }
        }
    }

    private String generateTeamCode() {
        while (true) {
            String code = "TEAM" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
            if (!teamGroupRepository.existsByTeamCodeAndIsDeletedFalse(code)) {
                return code;
            }
        }
    }
}
