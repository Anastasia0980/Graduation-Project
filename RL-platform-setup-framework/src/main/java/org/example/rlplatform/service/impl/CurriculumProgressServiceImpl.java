package org.example.rlplatform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.rlplatform.Repository.AssignmentCurriculumProgressRepository;
import org.example.rlplatform.entity.AssignmentCurriculumProgress;
import org.example.rlplatform.service.CurriculumProgressService;
import org.example.rlplatform.vo.CurriculumProgressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CurriculumProgressServiceImpl implements CurriculumProgressService {

    private static final Pattern TASK_ID_PATTERN = Pattern.compile("^T(10|[1-9])$", Pattern.CASE_INSENSITIVE);

    private final AssignmentCurriculumProgressRepository progressRepository;

    public CurriculumProgressServiceImpl(AssignmentCurriculumProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    @Override
    @Transactional
    public String resolveNextTaskId(Integer studentId, Integer assignmentId) {
        AssignmentCurriculumProgress p = getOrCreate(studentId, assignmentId);
        int h = zeroIfNull(p.getHighestPassedTaskIndex());
        if (h >= 10) {
            return "T10";
        }
        return "T" + (h + 1);
    }

    @Override
    @Transactional
    public void recordPassedStage(Integer studentId, Integer assignmentId, String taskIdNormalized) {
        int k = parseTaskIndex(taskIdNormalized);
        if (k < 1) {
            return;
        }
        AssignmentCurriculumProgress p = progressRepository
                .findByStudentIdAndAssignmentId(studentId, assignmentId)
                .orElse(null);
        if (p == null) {
            log.warn("Curriculum recordPassedStage: no progress row student={} assignment={}", studentId, assignmentId);
            return;
        }
        int h = zeroIfNull(p.getHighestPassedTaskIndex());
        int expected = (h >= 10) ? 10 : (h + 1);
        if (k != expected) {
            log.warn("Curriculum recordPassedStage: task mismatch student={} assignment={} expected=T{} got=T{}",
                    studentId, assignmentId, expected, k);
            return;
        }
        if (k > h) {
            p.setHighestPassedTaskIndex(k);
            p.setUpdateTime(LocalDateTime.now());
            progressRepository.save(p);
            log.info("Curriculum advanced student={} assignment={} highestPassed=T{}", studentId, assignmentId, k);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CurriculumProgressVO getProgress(Integer studentId, Integer assignmentId) {
        return progressRepository.findByStudentIdAndAssignmentId(studentId, assignmentId)
                .map(p -> {
                    int h = zeroIfNull(p.getHighestPassedTaskIndex());
                    String next = h >= 10 ? "T10" : "T" + (h + 1);
                    return new CurriculumProgressVO(h, next);
                })
                .orElseGet(() -> new CurriculumProgressVO(0, "T1"));
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

    /**
     * @return 1…10，非法返回 -1
     */
    private static int parseTaskIndex(String taskId) {
        if (taskId == null) {
            return -1;
        }
        String t = taskId.trim().toUpperCase(Locale.ROOT);
        Matcher m = TASK_ID_PATTERN.matcher(t);
        if (!m.matches()) {
            return -1;
        }
        return Integer.parseInt(m.group(1));
    }
}
