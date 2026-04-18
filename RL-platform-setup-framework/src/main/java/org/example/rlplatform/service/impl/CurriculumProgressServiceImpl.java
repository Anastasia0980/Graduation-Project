package org.example.rlplatform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.rlplatform.Repository.AssignmentCurriculumProgressRepository;
import org.example.rlplatform.entity.AssignmentCurriculumProgress;
import org.example.rlplatform.entity.CurriculumStageConfig;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.ExperimentConfig;
import org.example.rlplatform.entity.PublicationStatus;
import org.example.rlplatform.service.CurriculumProgressService;
import org.example.rlplatform.service.ExperimentAssignmentService;
import org.example.rlplatform.vo.CurriculumProgressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CurriculumProgressServiceImpl implements CurriculumProgressService {

    private static final Pattern TASK_ID_LEGACY_PATTERN = Pattern.compile("^T(10|[1-9])$", Pattern.CASE_INSENSITIVE);

    private final AssignmentCurriculumProgressRepository progressRepository;
    private final ExperimentAssignmentService experimentAssignmentService;

    public CurriculumProgressServiceImpl(AssignmentCurriculumProgressRepository progressRepository,
                                         ExperimentAssignmentService experimentAssignmentService) {
        this.progressRepository = progressRepository;
        this.experimentAssignmentService = experimentAssignmentService;
    }

    @Override
    @Transactional
    public String resolveNextTaskId(Integer studentId, Integer assignmentId) {
        ExperimentConfig cfg = loadAssignmentConfig(assignmentId);
        AssignmentCurriculumProgress p = getOrCreate(studentId, assignmentId);
        int h = zeroIfNull(p.getHighestPassedTaskIndex());

        if (cfg != null && cfg.getCurriculumStages() != null && !cfg.getCurriculumStages().isEmpty()) {
            List<CurriculumStageConfig> stages = cfg.getCurriculumStages();
            int n = stages.size();
            if (n == 0) {
                throw new IllegalStateException("课程关卡为空");
            }
            if (h >= n) {
                return stages.get(n - 1).getStageId();
            }
            return stages.get(h).getStageId();
        }

        if (h >= 10) {
            return "T10";
        }
        return "T" + (h + 1);
    }

    @Override
    @Transactional
    public void recordPassedStage(Integer studentId, Integer assignmentId, String taskIdNormalized) {
        ExperimentConfig cfg = loadAssignmentConfig(assignmentId);
        AssignmentCurriculumProgress p = progressRepository
                .findByStudentIdAndAssignmentId(studentId, assignmentId)
                .orElse(null);
        if (p == null) {
            log.warn("Curriculum recordPassedStage: no progress row student={} assignment={}", studentId, assignmentId);
            return;
        }

        if (cfg != null && cfg.getCurriculumStages() != null && !cfg.getCurriculumStages().isEmpty()) {
            recordCurriculumStage(cfg.getCurriculumStages(), p, taskIdNormalized);
            return;
        }

        recordLegacyT(taskIdNormalized, p);
    }

    private void recordCurriculumStage(List<CurriculumStageConfig> stages, AssignmentCurriculumProgress p, String taskIdNormalized) {
        int n = stages.size();
        if (n == 0) {
            return;
        }
        int h = zeroIfNull(p.getHighestPassedTaskIndex());
        String expected = h >= n ? stages.get(n - 1).getStageId() : stages.get(h).getStageId();
        if (taskIdNormalized == null || !expected.equals(taskIdNormalized.trim())) {
            log.warn("Curriculum recordPassedStage: stage mismatch expected={} got={}", expected, taskIdNormalized);
            return;
        }
        if (h >= n) {
            return;
        }
        p.setHighestPassedTaskIndex(h + 1);
        p.setUpdateTime(LocalDateTime.now());
        progressRepository.save(p);
        log.info("Curriculum advanced student={} assignment={} passedCount={}", p.getStudentId(), p.getAssignmentId(), h + 1);
    }

    private void recordLegacyT(String taskIdNormalized, AssignmentCurriculumProgress p) {
        int k = parseTaskIndex(taskIdNormalized);
        if (k < 1) {
            return;
        }
        int h = zeroIfNull(p.getHighestPassedTaskIndex());
        int expected = (h >= 10) ? 10 : (h + 1);
        if (k != expected) {
            log.warn("Curriculum recordPassedStage: task mismatch expected=T{} got=T{}", expected, k);
            return;
        }
        if (k > h) {
            p.setHighestPassedTaskIndex(k);
            p.setUpdateTime(LocalDateTime.now());
            progressRepository.save(p);
            log.info("Curriculum advanced student={} assignment={} highestPassed=T{}", p.getStudentId(), p.getAssignmentId(), k);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CurriculumProgressVO getProgress(Integer studentId, Integer assignmentId) {
        ExperimentAssignment assignment = experimentAssignmentService.getById(assignmentId);
        PublicationStatus pub = assignment == null ? PublicationStatus.PUBLISHED : assignment.getEffectivePublicationStatus();
        String pubName = pub.name();

        final ExperimentConfig cfg = assignment == null ? null : assignment.getConfig();
        int totalStages = 10;
        if (cfg != null && cfg.getCurriculumStages() != null && !cfg.getCurriculumStages().isEmpty()) {
            totalStages = cfg.getCurriculumStages().size();
        }
        final int total = totalStages;
        final String publication = pubName;

        return progressRepository.findByStudentIdAndAssignmentId(studentId, assignmentId)
                .map(p -> {
                    int h = zeroIfNull(p.getHighestPassedTaskIndex());
                    String next;
                    if (cfg != null && cfg.getCurriculumStages() != null && !cfg.getCurriculumStages().isEmpty()) {
                        List<CurriculumStageConfig> stages = cfg.getCurriculumStages();
                        int n = stages.size();
                        if (n == 0) {
                            next = "";
                        } else if (h >= n) {
                            next = stages.get(n - 1).getStageId();
                        } else {
                            next = stages.get(h).getStageId();
                        }
                    } else {
                        next = h >= 10 ? "T10" : "T" + (h + 1);
                    }
                    return new CurriculumProgressVO(h, next, total, publication);
                })
                .orElseGet(() -> {
                    String next;
                    if (cfg != null && cfg.getCurriculumStages() != null && !cfg.getCurriculumStages().isEmpty()) {
                        List<CurriculumStageConfig> stages = cfg.getCurriculumStages();
                        next = stages.isEmpty() ? "" : stages.get(0).getStageId();
                    } else {
                        next = "T1";
                    }
                    return new CurriculumProgressVO(0, next, total, publication);
                });
    }

    private ExperimentConfig loadAssignmentConfig(Integer assignmentId) {
        ExperimentAssignment a = experimentAssignmentService.getById(assignmentId);
        return a == null ? null : a.getConfig();
    }

    private AssignmentCurriculumProgress getOrCreate(Integer studentId, Integer assignmentId) {
        return progressRepository.findByStudentIdAndAssignmentId(studentId, assignmentId)
                .orElseGet(() -> {
                    AssignmentCurriculumProgress n = new AssignmentCurriculumProgress();
                    n.setStudentId(studentId);
                    n.setAssignmentId(assignmentId);
                    n.setHighestPassedTaskIndex(0);
                    n.setUpdateTime(LocalDateTime.now());
                    return progressRepository.save(n);
                });
    }

    private static int zeroIfNull(Integer v) {
        return v == null ? 0 : v;
    }

    private static int parseTaskIndex(String taskId) {
        if (taskId == null) {
            return -1;
        }
        String t = taskId.trim().toUpperCase(Locale.ROOT);
        Matcher m = TASK_ID_LEGACY_PATTERN.matcher(t);
        if (!m.matches()) {
            return -1;
        }
        return Integer.parseInt(m.group(1));
    }
}
