package org.example.rlplatform.controller;

import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.Repository.UserRepository;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.BaselineOption;
import org.example.rlplatform.service.CurriculumProgressService;
import org.example.rlplatform.entity.UserRole;
import org.example.rlplatform.service.EvaluationService;
import org.example.rlplatform.service.BaselineService;
import org.example.rlplatform.service.ExperimentAssignmentService;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.service.impl.LocalFileStorageService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.example.rlplatform.vo.TaskOverviewVO;
import org.example.rlplatform.vo.CurriculumProgressVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
public class ExperimentAssignmentController {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExperimentAssignmentService experimentAssignmentService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocalFileStorageService localFileStorageService;

    @Autowired
    private BaselineService baselineService;

    @Autowired
    private CurriculumProgressService curriculumProgressService;

    @PostMapping("class/{classId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Integer> create(@PathVariable Integer classId, @RequestBody ExperimentAssignment experimentAssignment) {
        Integer id = experimentAssignmentService.create(classId, experimentAssignment);
        return Result.success(id);
    }

    @PostMapping("assignments/{assignmentId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> publish(@PathVariable Integer assignmentId) {
        experimentAssignmentService.publish(assignmentId);
        return Result.success();
    }

    @PatchMapping("assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> update(@PathVariable Integer assignmentId, @RequestBody ExperimentAssignment experimentAssignment) {
        experimentAssignmentService.update(assignmentId, experimentAssignment);
        return Result.success();
    }


    @PostMapping("assignments/icon")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<String> uploadTaskIcon(@RequestParam("file") MultipartFile file) {
        return Result.success(localFileStorageService.storeImage(file, "task-icons"));
    }

    @GetMapping("me/assignments")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<ExperimentAssignment>> listStuAssignments(
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(experimentAssignmentService.listStuAssignments(pageNum, pageSize));
    }

    @GetMapping("teacher/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Page<ExperimentAssignment>> listTeaAssignments(
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(experimentAssignmentService.listTeaAssignments(pageNum, pageSize));
    }

    @GetMapping("teacher/assignments/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<TaskOverviewVO>> listTeacherAssignmentOverview() {
        Page<ExperimentAssignment> page = experimentAssignmentService.listTeaAssignments(0, 1000);
        List<ExperimentAssignment> assignments = page.getContent() == null ? Collections.emptyList() : page.getContent();

        List<TaskOverviewVO> voList = new ArrayList<>();

        for (ExperimentAssignment assignment : assignments) {
            TaskOverviewVO vo = new TaskOverviewVO();
            vo.setAssignmentId(assignment.getId());
            vo.setTaskName(assignment.getTitle());
            vo.setClassName(
                    assignment.getStudentClass() == null
                            ? "未分配班级"
                            : assignment.getStudentClass().getName()
            );
            vo.setDeadline(assignment.getDeadline());

            LocalDateTime now = LocalDateTime.now();
            vo.setStatus(
                    assignment.getDeadline() != null && assignment.getDeadline().isBefore(now)
                            ? "已结束"
                            : "进行中"
            );

            Integer classId = assignment.getStudentClass() == null ? null : assignment.getStudentClass().getId();
            List<User> classStudents = classId == null
                    ? Collections.emptyList()
                    : userRepository.findByStudentClass_IdAndRoleAndIsDeletedFalse(classId, UserRole.STUDENT);

            List<Evaluation> evaluations = evaluationRepository.findByAssignmentIdOrderByCreateTimeDesc(assignment.getId());
            Set<Integer> classStudentIdSet = classStudents.stream()
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            List<Evaluation> studentEvaluations = evaluations.stream()
                    .filter(item -> item.getStudentId() != null && classStudentIdSet.contains(item.getStudentId()))
                    .collect(Collectors.toList());

            Set<Integer> submittedStudentIdSet = studentEvaluations.stream()
                    .map(Evaluation::getStudentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            List<String> submittedStudentNames = classStudents.stream()
                    .filter(student -> submittedStudentIdSet.contains(student.getId()))
                    .map(User::getUsername)
                    .collect(Collectors.toList());

            List<String> unsubmittedStudentNames = classStudents.stream()
                    .filter(student -> !submittedStudentIdSet.contains(student.getId()))
                    .map(User::getUsername)
                    .collect(Collectors.toList());

            vo.setSubmittedCount(submittedStudentNames.size());
            vo.setUnsubmittedCount(unsubmittedStudentNames.size());
            vo.setTotalSubmissionCount(studentEvaluations.size());
            vo.setSubmittedStudentNames(submittedStudentNames);
            vo.setUnsubmittedStudentNames(unsubmittedStudentNames);

            voList.add(vo);
        }

        voList.sort((a, b) -> {
            if (a.getDeadline() == null && b.getDeadline() == null) return 0;
            if (a.getDeadline() == null) return 1;
            if (b.getDeadline() == null) return -1;
            return b.getDeadline().compareTo(a.getDeadline());
        });

        return Result.success(voList);
    }

    @GetMapping("class/{classId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Page<ExperimentAssignment>> listAssignmentsByClass(
            @PathVariable Integer classId,
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(experimentAssignmentService.listAssignmentsByClass(classId, pageNum, pageSize));
    }

    @PostMapping("assignments/{assignmentId}/evaluations")
    public Result<Void> createEvaluation(
            @PathVariable Integer assignmentId,
            @RequestParam("model") MultipartFile model,
            @RequestParam("config") MultipartFile config
    ) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer studentId = (Integer) claims.get("id");
        checkCooldown(studentId, assignmentId.longValue(), 10L);
        evaluationService.runEvaluationByConfig(assignmentId, model, config);
        return Result.success();
    }

    @GetMapping("assignments/{assignmentId}/curriculum-progress")
    public Result<CurriculumProgressVO> curriculumProgress(@PathVariable Integer assignmentId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || claims.get("id") == null) {
            return Result.error("未登录或登录已失效");
        }
        Integer studentId = (Integer) claims.get("id");
        return Result.success(curriculumProgressService.getProgress(studentId, assignmentId));
    }

    @DeleteMapping("assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> softDelete(@PathVariable Integer assignmentId) {
        experimentAssignmentService.softDelete(assignmentId);
        return Result.success();
    }

    /**
     * 获取某个任务的 baseline catalog（基于任务环境与磁盘中的 baseline 文件）
     */
    @GetMapping("assignments/{assignmentId}/baseline-catalog")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, List<BaselineOption>>> baselineCatalog(@PathVariable Integer assignmentId) {
        ExperimentAssignment assignment = experimentAssignmentService.getById(assignmentId);
        if (assignment == null) {
            return Result.error("实验任务不存在或已删除");
        }

        String environment = assignment.getEnvironment();
        Map<String, List<BaselineOption>> catalog = baselineService.getBaselineCatalogByEnvironment(environment);
        return Result.success(catalog);
    }

    /**
     * 按环境获取 baseline catalog（发布页无需 assignmentId，也可以直接拿可选项）
     */
    @GetMapping("baselines/catalog")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, List<BaselineOption>>> baselineCatalogByEnvironment(
            @RequestParam("environment") String environment
    ) {
        Map<String, List<BaselineOption>> catalog = baselineService.getBaselineCatalogByEnvironment(environment);
        return Result.success(catalog);
    }

    /**
     * 上传 baseline.pth 到固定基目录，并写入 baseline 表（只影响 baseline 资源，不影响任务已选配置）
     * 上传完成后前端可重新拉取 catalog，并执行“加入/删除=取消选择”语义。
     */
    @PostMapping("assignments/{assignmentId}/baseline-upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<BaselineOption> uploadBaseline(
            @PathVariable Integer assignmentId,
            @RequestParam("taskId") String taskId,
            @RequestParam("algorithm") String algorithm,
            @RequestParam("model") MultipartFile model
    ) {
        ExperimentAssignment assignment = experimentAssignmentService.getById(assignmentId);
        if (assignment == null) {
            return Result.error("实验任务不存在或已删除");
        }
        try {
            BaselineOption option = baselineService.uploadBaseline(assignment.getEnvironment(), taskId, algorithm, model);
            return Result.success(option);
        } catch (Exception e) {
            return Result.error(e.getMessage() != null ? e.getMessage() : "baseline 上传失败");
        }
    }

    /**
     * 上传 baseline.pth（不依赖 assignmentId，发布页可用）
     */
    @PostMapping("baselines/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<BaselineOption> uploadBaselineByEnvironment(
            @RequestParam("environment") String environment,
            @RequestParam("taskId") String taskId,
            @RequestParam("algorithm") String algorithm,
            @RequestParam("model") MultipartFile model
    ) {
        try {
            BaselineOption option = baselineService.uploadBaseline(environment, taskId, algorithm, model);
            return Result.success(option);
        } catch (Exception e) {
            return Result.error(e.getMessage() != null ? e.getMessage() : "baseline 上传失败");
        }
    }

    /**
     * baseline 软删除：仅将 baseline 表记录置为 isDeleted=true（不删除文件）
     */
    @DeleteMapping("baselines/soft-delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> softDeleteBaseline(
            @RequestParam("environment") String environment,
            @RequestParam("taskId") String taskId,
            @RequestParam("algorithm") String algorithm
    ) {
        baselineService.softDeleteBaseline(environment, taskId, algorithm);
        return Result.success();
    }

    public void checkCooldown(Integer studentId, Long assignmentId, long cooldownSeconds) {
        String key = "eval:cooldown:" + studentId + ":" + assignmentId;
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", cooldownSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(success)) {
            throw new RuntimeException("操作过于频繁，请稍后再试");
        }
    }
}