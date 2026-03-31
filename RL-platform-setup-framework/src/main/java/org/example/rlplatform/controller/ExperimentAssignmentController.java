package org.example.rlplatform.controller;

import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.Repository.UserRepository;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;
import org.example.rlplatform.service.EvaluationService;
import org.example.rlplatform.service.ExperimentAssignmentService;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.example.rlplatform.vo.TaskOverviewVO;
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

    @PostMapping("class/{classId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> create(@PathVariable Integer classId, @RequestBody ExperimentAssignment experimentAssignment) {
        experimentAssignmentService.create(classId, experimentAssignment);
        return Result.success();
    }

    @PatchMapping("assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> update(@PathVariable Integer assignmentId, @RequestBody ExperimentAssignment experimentAssignment) {
        experimentAssignmentService.update(assignmentId, experimentAssignment);
        return Result.success();
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

    @DeleteMapping("assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> softDelete(@PathVariable Integer assignmentId) {
        experimentAssignmentService.softDelete(assignmentId);
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