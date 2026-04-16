package org.example.rlplatform.controller;

import org.example.rlplatform.Repository.ScoreExportRecordRepository;
import org.example.rlplatform.Repository.UserRepository;
import org.example.rlplatform.entity.EvaluationMode;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.LeaderBoard;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.entity.ScoreExportRecord;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;
import org.example.rlplatform.service.ExperimentAssignmentService;
import org.example.rlplatform.service.LeaderBoardService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.example.rlplatform.vo.ScoreExportDataVO;
import org.example.rlplatform.vo.ScoreExportRowVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher/exports")
public class ScoreExportController {

    @Autowired
    private ExperimentAssignmentService experimentAssignmentService;

    @Autowired
    private LeaderBoardService leaderBoardService;

    @Autowired
    private ScoreExportRecordRepository scoreExportRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<ScoreExportRecord>> listRecords() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer teacherId = (Integer) claims.get("id");
        return Result.success(scoreExportRecordRepository.findByTeacherIdOrderByExportTimeDesc(teacherId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<ScoreExportDataVO> export(@RequestBody Map<String, Object> body) {
        Integer assignmentId = body.get("assignmentId") == null
                ? null
                : Integer.valueOf(String.valueOf(body.get("assignmentId")));

        if (assignmentId == null) {
            return Result.error("assignmentId不能为空");
        }

        ExperimentAssignment assignment = experimentAssignmentService.getById(assignmentId);
        if (assignment == null) {
            return Result.error("任务不存在");
        }

        ScoreExportDataVO vo = new ScoreExportDataVO();
        vo.setAssignmentId(assignmentId);
        vo.setTaskName(assignment.getTitle());
        vo.setClassName(
                assignment.getStudentClass() == null
                        ? "未分配班级"
                        : assignment.getStudentClass().getName()
        );
        vo.setMode(assignment.getEvaluationMode().name());

        List<ScoreExportRowVO> rows = new ArrayList<>();

        if (assignment.getEvaluationMode() == EvaluationMode.SINGLE) {
            ScoreExportRowVO row = new ScoreExportRowVO();
            row.setRank(1);

            String displayName = "学生A";
            if (assignment.getStudentClass() != null) {
                List<User> students = userRepository.findByStudentClass_IdAndRoleAndIsDeletedFalse(
                        assignment.getStudentClass().getId(),
                        UserRole.STUDENT
                );
                if (!students.isEmpty() && students.get(0).getUsername() != null && !students.get(0).getUsername().isBlank()) {
                    displayName = students.get(0).getUsername();
                }
            }

            row.setName(displayName);
            row.setLevelCount(4);
            row.setClearTime("2小时15分");
            rows.add(row);
        } else if (assignment.getEvaluationMode() == EvaluationMode.TEAM) {
            Page<LeaderBoard> page = leaderBoardService.listTeam(assignmentId, 0, 1000);
            List<LeaderBoard> rankingRows = page.getContent() == null ? Collections.emptyList() : page.getContent();

            for (LeaderBoard item : rankingRows) {
                ScoreExportRowVO row = new ScoreExportRowVO();
                row.setRank(item.getRank());
                row.setName(item.getTeamName());
                row.setCaptainName(item.getCaptainName());
                row.setMember1Name(item.getMember1Name());
                row.setMember2Name(item.getMember2Name());
                row.setLadderScore(item.getLadderScore());
                row.setMatchCount(item.getMatchCount());
                row.setWinCount(item.getWinCount());
                row.setLoseCount(item.getLoseCount());
                row.setDrawCount(item.getDrawCount());
                rows.add(row);
            }
        } else {
            Page<LeaderBoard> page = leaderBoardService.list(assignmentId, 0, 1000);
            List<LeaderBoard> rankingRows = page.getContent() == null ? Collections.emptyList() : page.getContent();

            for (LeaderBoard item : rankingRows) {
                ScoreExportRowVO row = new ScoreExportRowVO();
                row.setRank(item.getRank());
                row.setName(item.getNickname());
                row.setLadderScore(item.getLadderScore());
                row.setMatchCount(item.getMatchCount());
                row.setWinCount(item.getWinCount());
                row.setLoseCount(item.getLoseCount());
                row.setDrawCount(item.getDrawCount());
                rows.add(row);
            }
        }

        vo.setRows(rows);

        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer teacherId = (Integer) claims.get("id");

        ScoreExportRecord record = new ScoreExportRecord();
        record.setTeacherId(teacherId);
        record.setAssignmentId(assignmentId);
        record.setClassName(vo.getClassName());
        record.setTaskName(vo.getTaskName());
        record.setStatus("已完成");
        record.setExportTime(LocalDateTime.now());
        scoreExportRecordRepository.save(record);

        return Result.success(vo);
    }
}
