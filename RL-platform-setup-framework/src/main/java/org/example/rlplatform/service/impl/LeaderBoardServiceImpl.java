package org.example.rlplatform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rlplatform.Repository.BattleParticipantRepository;
import org.example.rlplatform.Repository.BattleModelSubmissionRepository;
import org.example.rlplatform.Repository.AssignmentCurriculumProgressRepository;
import org.example.rlplatform.Repository.TeamGroupRepository;
import org.example.rlplatform.Repository.TeamMemberRepository;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.Repository.UserRepository;
import org.example.rlplatform.entity.BattleParticipant;
import org.example.rlplatform.entity.BattleModelSubmission;
import org.example.rlplatform.entity.AssignmentCurriculumProgress;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.EvaluationMode;
import org.example.rlplatform.entity.EvaluationResult;
import org.example.rlplatform.entity.EvaluationStatus;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.LeaderBoard;
import org.example.rlplatform.entity.TeamGroup;
import org.example.rlplatform.entity.TeamMember;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.service.LeaderBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDateTime;

@Service
public class LeaderBoardServiceImpl implements LeaderBoardService {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationResultRepository evaluationResultRepository;

    @Autowired
    private BattleParticipantRepository battleParticipantRepository;

    @Autowired
    private BattleModelSubmissionRepository battleModelSubmissionRepository;

    @Autowired
    private ExperimentAssignmentRepository experimentAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamGroupRepository teamGroupRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private AssignmentCurriculumProgressRepository assignmentCurriculumProgressRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Page<LeaderBoard> list(Integer assignmentId, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        if (assignment == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        if (assignment.getEvaluationMode() == EvaluationMode.SINGLE) {
            return listSingle(assignmentId, pageable);
        }
        if (assignment.getEvaluationMode() != EvaluationMode.VERSUS) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Evaluation> evaluations = evaluationRepository.findByAssignmentIdOrderByCreateTimeDesc(assignmentId);
        if (evaluations == null || evaluations.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Map<Long, EvaluationResult> resultMap = new HashMap<>();
        Map<Long, BattleParticipant> participantMap = new HashMap<>();

        List<Long> evaluationIds = new ArrayList<>();
        for (Evaluation evaluation : evaluations) {
            evaluationIds.add(evaluation.getId());
        }

        List<EvaluationResult> resultList = evaluationResultRepository.findByEvaluationIdIn(evaluationIds);
        for (EvaluationResult result : resultList) {
            resultMap.put(result.getEvaluationId(), result);
        }

        List<BattleParticipant> participantList = battleParticipantRepository.findByEvaluationIdIn(evaluationIds);
        for (BattleParticipant participant : participantList) {
            participantMap.put(participant.getEvaluationId(), participant);
        }

        Map<Long, BattleModelSubmission> submissionMap = buildSubmissionMap(participantMap.values());
        Map<String, PlayerStats> statsMap = new LinkedHashMap<>();

        for (Evaluation evaluation : evaluations) {
            if (evaluation.getStatus() != EvaluationStatus.FINISHED) {
                continue;
            }

            BattleParticipant participant = participantMap.get(evaluation.getId());
            if (participant == null) {
                continue;
            }

            if (!"HUMAN".equalsIgnoreCase(participant.getOpponentType())) {
                continue;
            }

            if (participant.getStudent1Id() == null || participant.getStudent2Id() == null) {
                continue;
            }

            EvaluationResult evaluationResult = resultMap.get(evaluation.getId());
            if (evaluationResult == null || evaluationResult.getDetailedResults() == null || evaluationResult.getDetailedResults().isBlank()) {
                continue;
            }

            JsonNode root = parseJson(evaluationResult.getDetailedResults());
            if (root == null) {
                continue;
            }

            Integer winner = root.has("winner") ? root.path("winner").asInt() : null;
            if (winner == null) {
                continue;
            }

            PlayerStats stats1 = resolvePlayerStats(statsMap, "student", participant.getStudent1Id().intValue(),
                    participant.getStudent1SubmissionId(), participant.getStudent1ModelName(), submissionMap);
            PlayerStats stats2 = resolvePlayerStats(statsMap, "student", participant.getStudent2Id().intValue(),
                    participant.getStudent2SubmissionId(), participant.getStudent2ModelName(), submissionMap);

            stats1.matchCount += 1;
            stats2.matchCount += 1;

            if (winner == 1) {
                stats1.winCount += 1;
                stats2.loseCount += 1;
                stats1.addOpponent(stats2.statsKey, 1, 1, 0, 0);
                stats2.addOpponent(stats1.statsKey, 1, 0, 1, 0);
            } else if (winner == 2) {
                stats2.winCount += 1;
                stats1.loseCount += 1;
                stats1.addOpponent(stats2.statsKey, 1, 0, 1, 0);
                stats2.addOpponent(stats1.statsKey, 1, 1, 0, 0);
            } else {
                stats1.drawCount += 1;
                stats2.drawCount += 1;
                stats1.addOpponent(stats2.statsKey, 1, 0, 0, 1);
                stats2.addOpponent(stats1.statsKey, 1, 0, 0, 1);
            }
        }

        if (statsMap.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // 第一步：基础战绩分 S（按场次计算，不按30轮内部局数）
        for (PlayerStats stats : statsMap.values()) {
            if (stats.matchCount <= 0) {
                stats.baseScore = 0.0;
            } else {
                stats.baseScore = (stats.winCount + 0.5 * stats.drawCount) / stats.matchCount;
            }
        }

        // 第二步：对手强度 O，多样性 D，重复挑战惩罚 P，活跃度修正
        for (PlayerStats stats : statsMap.values()) {
            if (stats.matchCount <= 0) {
                stats.opponentStrengthScore = 0.0;
                stats.diversityScore = 0.0;
                stats.penaltyScore = 0.0;
                stats.activityFactor = 0.0;
                stats.finalScore = 0.0;
                stats.ladderScore = 0;
                continue;
            }

            double weightedOpponentStrength = 0.0;
            for (OpponentStats opponentStats : stats.opponentStatsMap.values()) {
                PlayerStats opponent = statsMap.get(opponentStats.opponentKey);
                double opponentBaseScore = opponent == null ? 0.0 : opponent.baseScore;
                double myPerformanceAgainstOpponent =
                        opponentStats.matchCount <= 0
                                ? 0.0
                                : (opponentStats.winCount + 0.5 * opponentStats.drawCount) / opponentStats.matchCount;

                weightedOpponentStrength += myPerformanceAgainstOpponent * opponentBaseScore * opponentStats.matchCount;
            }

            stats.opponentStrengthScore = weightedOpponentStrength / stats.matchCount;
            stats.diversityScore = (double) stats.opponentStatsMap.size() / stats.matchCount;

            int repeatedPenaltyCount = 0;
            for (OpponentStats opponentStats : stats.opponentStatsMap.values()) {
                repeatedPenaltyCount += Math.max(0, opponentStats.matchCount - 2);
            }
            stats.penaltyScore = (double) repeatedPenaltyCount / stats.matchCount;
            stats.activityFactor = Math.min(1.0, stats.matchCount / 5.0);

            double rawScore =
                    0.45 * stats.baseScore +
                            0.35 * stats.opponentStrengthScore +
                            0.15 * stats.diversityScore -
                            0.05 * stats.penaltyScore;

            stats.finalScore = Math.max(0.0, rawScore * stats.activityFactor);
            stats.ladderScore = 1000 + Math.max(0, (int) Math.round(stats.finalScore * 1000));
        }

        List<PlayerStats> sortedStats = new ArrayList<>(statsMap.values());
        sortedStats.sort((a, b) -> {
            if (b.ladderScore != a.ladderScore) {
                return Integer.compare(b.ladderScore, a.ladderScore);
            }
            if (Double.compare(b.opponentStrengthScore, a.opponentStrengthScore) != 0) {
                return Double.compare(b.opponentStrengthScore, a.opponentStrengthScore);
            }
            if (a.matchCount != b.matchCount) {
                return Integer.compare(b.matchCount, a.matchCount);
            }
            return compareStatsOwner(a, b);
        });

        List<Integer> studentIds = new ArrayList<>();
        for (PlayerStats stats : sortedStats) {
            studentIds.add(stats.studentId);
        }

        Map<Integer, User> userMap = new HashMap<>();
        for (User user : userRepository.findAllById(studentIds)) {
            userMap.put(user.getId(), user);
        }

        List<LeaderBoard> allRows = new ArrayList<>();
        for (int i = 0; i < sortedStats.size(); i++) {
            PlayerStats stats = sortedStats.get(i);
            User user = userMap.get(stats.studentId);

            String displayName = "未知学生";
            if (user != null) {
                if (user.getUsername() != null && !user.getUsername().isBlank()) {
                    displayName = user.getUsername();
                } else if (user.getNickname() != null && !user.getNickname().isBlank()) {
                    displayName = user.getNickname();
                }
            }

            LeaderBoard row = new LeaderBoard();
            row.setRank(i + 1);
            row.setStudentId(stats.studentId);
            row.setSubmissionId(stats.submissionId);
            row.setModelName(stats.modelName);
            row.setNickname(displayName);
            row.setBestScore(stats.ladderScore);
            row.setLadderScore(stats.ladderScore);
            row.setWinCount(stats.winCount);
            row.setLoseCount(stats.loseCount);
            row.setDrawCount(stats.drawCount);
            row.setMatchCount(stats.matchCount);
            allRows.add(row);
        }

        int total = allRows.size();
        int start = Math.min(pageNum * pageSize, total);
        int end = Math.min(start + pageSize, total);
        List<LeaderBoard> pageRows = allRows.subList(start, end);

        return new PageImpl<>(pageRows, pageable, total);
    }

    private Page<LeaderBoard> listSingle(Integer assignmentId, Pageable pageable) {
        List<AssignmentCurriculumProgress> progressList = assignmentCurriculumProgressRepository.findByAssignmentId(assignmentId);
        if (progressList == null || progressList.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Map<Integer, AssignmentCurriculumProgress> bestByStudent = new HashMap<>();
        for (AssignmentCurriculumProgress progress : progressList) {
            if (progress == null || progress.getStudentId() == null) {
                continue;
            }
            Integer studentId = progress.getStudentId();
            AssignmentCurriculumProgress existing = bestByStudent.get(studentId);
            if (existing == null) {
                bestByStudent.put(studentId, progress);
                continue;
            }
            int currentLevel = zeroIfNull(progress.getHighestPassedTaskIndex());
            int existingLevel = zeroIfNull(existing.getHighestPassedTaskIndex());
            if (currentLevel > existingLevel) {
                bestByStudent.put(studentId, progress);
                continue;
            }
            if (currentLevel == existingLevel) {
                LocalDateTime currentTime = progress.getUpdateTime();
                LocalDateTime existingTime = existing.getUpdateTime();
                if (isEarlier(currentTime, existingTime)) {
                    bestByStudent.put(studentId, progress);
                }
            }
        }

        if (bestByStudent.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<AssignmentCurriculumProgress> ranked = new ArrayList<>(bestByStudent.values());
        ranked.sort((a, b) -> {
            int levelA = zeroIfNull(a.getHighestPassedTaskIndex());
            int levelB = zeroIfNull(b.getHighestPassedTaskIndex());
            if (levelB != levelA) {
                return Integer.compare(levelB, levelA);
            }

            LocalDateTime timeA = a.getUpdateTime();
            LocalDateTime timeB = b.getUpdateTime();
            if (timeA != null && timeB != null && !timeA.equals(timeB)) {
                return timeA.compareTo(timeB);
            }
            if (timeA == null && timeB != null) {
                return 1;
            }
            if (timeA != null) {
                return -1;
            }
            return Integer.compare(a.getStudentId(), b.getStudentId());
        });

        List<Integer> studentIds = new ArrayList<>();
        for (AssignmentCurriculumProgress p : ranked) {
            studentIds.add(p.getStudentId());
        }
        Map<Integer, User> userMap = new HashMap<>();
        for (User user : userRepository.findAllById(studentIds)) {
            userMap.put(user.getId(), user);
        }

        List<LeaderBoard> allRows = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            AssignmentCurriculumProgress progress = ranked.get(i);
            User user = userMap.get(progress.getStudentId());
            String displayName = "未知学生";
            if (user != null) {
                if (user.getUsername() != null && !user.getUsername().isBlank()) {
                    displayName = user.getUsername();
                } else if (user.getNickname() != null && !user.getNickname().isBlank()) {
                    displayName = user.getNickname();
                }
            }

            LeaderBoard row = new LeaderBoard();
            row.setRank(i + 1);
            row.setStudentId(progress.getStudentId());
            row.setNickname(displayName);
            row.setLevelCount(zeroIfNull(progress.getHighestPassedTaskIndex()));
            row.setClearTime(progress.getUpdateTime() == null ? "--" : progress.getUpdateTime().toString());
            allRows.add(row);
        }

        int total = allRows.size();
        int start = Math.min((int) pageable.getOffset(), total);
        int end = Math.min(start + pageable.getPageSize(), total);
        return new PageImpl<>(allRows.subList(start, end), pageable, total);
    }

    private static int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private static boolean isEarlier(LocalDateTime current, LocalDateTime existing) {
        if (current == null) {
            return false;
        }
        if (existing == null) {
            return true;
        }
        return current.isBefore(existing);
    }

    @Override
    public Page<LeaderBoard> listTeam(Integer assignmentId, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        if (assignment == null || assignment.getEvaluationMode() != EvaluationMode.TEAM) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Evaluation> evaluations = evaluationRepository.findByAssignmentIdOrderByCreateTimeDesc(assignmentId);
        if (evaluations == null || evaluations.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Long> evaluationIds = new ArrayList<>();
        for (Evaluation evaluation : evaluations) {
            evaluationIds.add(evaluation.getId());
        }

        Map<Long, EvaluationResult> resultMap = new HashMap<>();
        for (EvaluationResult result : evaluationResultRepository.findByEvaluationIdIn(evaluationIds)) {
            resultMap.put(result.getEvaluationId(), result);
        }

        Map<Long, BattleParticipant> participantMap = new HashMap<>();
        for (BattleParticipant participant : battleParticipantRepository.findByEvaluationIdIn(evaluationIds)) {
            participantMap.put(participant.getEvaluationId(), participant);
        }
        Map<Long, BattleModelSubmission> submissionMap = buildSubmissionMap(participantMap.values());

        List<TeamGroup> teamGroups = teamGroupRepository.findByAssignmentIdAndIsDeletedFalseOrderByIdAsc(assignmentId);
        if (teamGroups == null || teamGroups.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Map<Integer, TeamGroup> captainTeamMap = new HashMap<>();
        Map<Integer, TeamGroup> teamGroupMap = new HashMap<>();
        for (TeamGroup teamGroup : teamGroups) {
            teamGroupMap.put(teamGroup.getId(), teamGroup);
            if (teamGroup.getCaptainStudentId() != null) {
                captainTeamMap.put(teamGroup.getCaptainStudentId(), teamGroup);
            }
        }

        Map<String, PlayerStats> statsMap = new LinkedHashMap<>();

        for (Evaluation evaluation : evaluations) {
            if (evaluation.getStatus() != EvaluationStatus.FINISHED) {
                continue;
            }

            BattleParticipant participant = participantMap.get(evaluation.getId());
            if (participant == null || !"HUMAN".equalsIgnoreCase(participant.getOpponentType())) {
                continue;
            }
            if (participant.getStudent1Id() == null || participant.getStudent2Id() == null) {
                continue;
            }

            TeamGroup team1 = captainTeamMap.get(participant.getStudent1Id().intValue());
            TeamGroup team2 = captainTeamMap.get(participant.getStudent2Id().intValue());
            if (team1 == null || team2 == null || Objects.equals(team1.getId(), team2.getId())) {
                continue;
            }

            EvaluationResult evaluationResult = resultMap.get(evaluation.getId());
            if (evaluationResult == null || evaluationResult.getDetailedResults() == null || evaluationResult.getDetailedResults().isBlank()) {
                continue;
            }

            JsonNode root = parseJson(evaluationResult.getDetailedResults());
            if (root == null || !root.has("winner")) {
                continue;
            }
            Integer winner = root.path("winner").asInt();

            Integer team1Id = team1.getId();
            Integer team2Id = team2.getId();
            PlayerStats stats1 = resolvePlayerStats(statsMap, "team", team1Id,
                    participant.getStudent1SubmissionId(), participant.getStudent1ModelName(), submissionMap);
            PlayerStats stats2 = resolvePlayerStats(statsMap, "team", team2Id,
                    participant.getStudent2SubmissionId(), participant.getStudent2ModelName(), submissionMap);
            stats1.matchCount += 1;
            stats2.matchCount += 1;

            if (winner == 1) {
                stats1.winCount += 1;
                stats2.loseCount += 1;
                stats1.addOpponent(stats2.statsKey, 1, 1, 0, 0);
                stats2.addOpponent(stats1.statsKey, 1, 0, 1, 0);
            } else if (winner == 2) {
                stats2.winCount += 1;
                stats1.loseCount += 1;
                stats1.addOpponent(stats2.statsKey, 1, 0, 1, 0);
                stats2.addOpponent(stats1.statsKey, 1, 1, 0, 0);
            } else {
                stats1.drawCount += 1;
                stats2.drawCount += 1;
                stats1.addOpponent(stats2.statsKey, 1, 0, 0, 1);
                stats2.addOpponent(stats1.statsKey, 1, 0, 0, 1);
            }
        }

        if (statsMap.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        for (PlayerStats stats : statsMap.values()) {
            if (stats.matchCount <= 0) {
                stats.baseScore = 0.0;
            } else {
                stats.baseScore = (stats.winCount + 0.5 * stats.drawCount) / stats.matchCount;
            }
        }

        for (PlayerStats stats : statsMap.values()) {
            if (stats.matchCount <= 0) {
                stats.opponentStrengthScore = 0.0;
                stats.diversityScore = 0.0;
                stats.penaltyScore = 0.0;
                stats.activityFactor = 0.0;
                stats.finalScore = 0.0;
                stats.ladderScore = 0;
                continue;
            }

            double weightedOpponentStrength = 0.0;
            for (OpponentStats opponentStats : stats.opponentStatsMap.values()) {
                PlayerStats opponent = statsMap.get(opponentStats.opponentKey);
                double opponentBaseScore = opponent == null ? 0.0 : opponent.baseScore;
                double myPerformanceAgainstOpponent = opponentStats.matchCount <= 0
                        ? 0.0
                        : (opponentStats.winCount + 0.5 * opponentStats.drawCount) / opponentStats.matchCount;
                weightedOpponentStrength += myPerformanceAgainstOpponent * opponentBaseScore * opponentStats.matchCount;
            }

            stats.opponentStrengthScore = weightedOpponentStrength / stats.matchCount;
            stats.diversityScore = (double) stats.opponentStatsMap.size() / stats.matchCount;

            int repeatedPenaltyCount = 0;
            for (OpponentStats opponentStats : stats.opponentStatsMap.values()) {
                repeatedPenaltyCount += Math.max(0, opponentStats.matchCount - 2);
            }
            stats.penaltyScore = (double) repeatedPenaltyCount / stats.matchCount;
            stats.activityFactor = Math.min(1.0, stats.matchCount / 5.0);

            double rawScore = 0.45 * stats.baseScore + 0.35 * stats.opponentStrengthScore + 0.15 * stats.diversityScore - 0.05 * stats.penaltyScore;
            stats.finalScore = Math.max(0.0, rawScore * stats.activityFactor);
            stats.ladderScore = 1000 + Math.max(0, (int) Math.round(stats.finalScore * 1000));
        }

        List<PlayerStats> sortedStats = new ArrayList<>(statsMap.values());
        sortedStats.sort((a, b) -> {
            if (b.ladderScore != a.ladderScore) {
                return Integer.compare(b.ladderScore, a.ladderScore);
            }
            if (Double.compare(b.opponentStrengthScore, a.opponentStrengthScore) != 0) {
                return Double.compare(b.opponentStrengthScore, a.opponentStrengthScore);
            }
            if (a.matchCount != b.matchCount) {
                return Integer.compare(b.matchCount, a.matchCount);
            }
            return compareStatsOwner(a, b);
        });

        List<LeaderBoard> allRows = new ArrayList<>();
        for (int i = 0; i < sortedStats.size(); i++) {
            PlayerStats stats = sortedStats.get(i);
            TeamGroup teamGroup = teamGroupMap.get(stats.studentId);
            if (teamGroup == null) {
                continue;
            }
            LeaderBoard row = new LeaderBoard();
            row.setRank(i + 1);
            row.setTeamId(teamGroup.getId().longValue());
            row.setTeamName(teamGroup.getTeamName());
            row.setSubmissionId(stats.submissionId);
            row.setModelName(stats.modelName);
            fillTeamMemberNames(row, teamGroup);
            row.setBestScore(stats.ladderScore);
            row.setLadderScore(stats.ladderScore);
            row.setWinCount(stats.winCount);
            row.setLoseCount(stats.loseCount);
            row.setDrawCount(stats.drawCount);
            row.setMatchCount(stats.matchCount);
            allRows.add(row);
        }

        int total = allRows.size();
        int start = Math.min(pageNum * pageSize, total);
        int end = Math.min(start + pageSize, total);
        List<LeaderBoard> pageRows = allRows.subList(start, end);
        return new PageImpl<>(pageRows, pageable, total);
    }

    private void fillTeamMemberNames(LeaderBoard row, TeamGroup teamGroup) {
        List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsDeletedFalseOrderByIdAsc(teamGroup.getId());
        List<Integer> userIds = new ArrayList<>();
        for (TeamMember member : members) {
            userIds.add(member.getStudentId());
        }

        Map<Integer, String> nameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userRepository.findAllById(userIds)) {
                String displayName = (user.getUsername() != null && !user.getUsername().isBlank())
                        ? user.getUsername()
                        : user.getNickname();
                nameMap.put(user.getId(), displayName == null || displayName.isBlank() ? "未知学生" : displayName);
            }
        }

        row.setCaptainName(nameMap.getOrDefault(teamGroup.getCaptainStudentId(), "未知学生"));
        List<String> others = new ArrayList<>();
        for (TeamMember member : members) {
            if (Objects.equals(member.getStudentId(), teamGroup.getCaptainStudentId())) {
                continue;
            }
            others.add(nameMap.getOrDefault(member.getStudentId(), "未知学生"));
        }
        row.setMember1Name(others.size() > 0 ? others.get(0) : "--");
        row.setMember2Name(others.size() > 1 ? others.get(1) : "--");
    }


    private JsonNode parseJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<Long, BattleModelSubmission> buildSubmissionMap(Collection<BattleParticipant> participants) {
        Set<Long> submissionIds = new HashSet<>();
        if (participants != null) {
            for (BattleParticipant participant : participants) {
                if (participant.getStudent1SubmissionId() != null) {
                    submissionIds.add(participant.getStudent1SubmissionId());
                }
                if (participant.getStudent2SubmissionId() != null) {
                    submissionIds.add(participant.getStudent2SubmissionId());
                }
            }
        }

        Map<Long, BattleModelSubmission> submissionMap = new HashMap<>();
        if (!submissionIds.isEmpty()) {
            for (BattleModelSubmission submission : battleModelSubmissionRepository.findAllById(submissionIds)) {
                submissionMap.put(submission.getId(), submission);
            }
        }
        return submissionMap;
    }

    private PlayerStats resolvePlayerStats(
            Map<String, PlayerStats> statsMap,
            String ownerType,
            Integer ownerId,
            Long submissionId,
            String participantModelName,
            Map<Long, BattleModelSubmission> submissionMap
    ) {
        BattleModelSubmission submission = submissionId == null ? null : submissionMap.get(submissionId);
        String modelName = resolveModelName(participantModelName, submission);
        String statsKey = buildStatsKey(ownerType, ownerId, submissionId, modelName);
        return statsMap.computeIfAbsent(statsKey, key -> new PlayerStats(statsKey, ownerId, submissionId, modelName));
    }

    private String resolveModelName(String participantModelName, BattleModelSubmission submission) {
        if (participantModelName != null && !participantModelName.isBlank()) {
            return participantModelName;
        }
        if (submission != null && submission.getModelName() != null && !submission.getModelName().isBlank()) {
            return submission.getModelName();
        }
        return "未命名模型";
    }

    private String buildStatsKey(String ownerType, Integer ownerId, Long submissionId, String modelName) {
        if (submissionId != null) {
            return "submission:" + submissionId;
        }
        return ownerType + ":" + ownerId + ":" + (modelName == null ? "" : modelName.trim());
    }

    private int compareStatsOwner(PlayerStats a, PlayerStats b) {
        int ownerCompare = Integer.compare(a.studentId, b.studentId);
        if (ownerCompare != 0) {
            return ownerCompare;
        }
        long submissionA = a.submissionId == null ? Long.MAX_VALUE : a.submissionId;
        long submissionB = b.submissionId == null ? Long.MAX_VALUE : b.submissionId;
        return Long.compare(submissionA, submissionB);
    }

    private static class PlayerStats {
        private final String statsKey;
        private final Integer studentId;
        private final Long submissionId;
        private final String modelName;

        private int winCount;
        private int loseCount;
        private int drawCount;
        private int matchCount;

        private double baseScore;
        private double opponentStrengthScore;
        private double diversityScore;
        private double penaltyScore;
        private double activityFactor;
        private double finalScore;
        private int ladderScore;

        private final Map<String, OpponentStats> opponentStatsMap = new HashMap<>();

        private PlayerStats(String statsKey, Integer studentId, Long submissionId, String modelName) {
            this.statsKey = statsKey;
            this.studentId = studentId;
            this.submissionId = submissionId;
            this.modelName = modelName;
        }

        private void addOpponent(String opponentKey, int matchInc, int wins, int loses, int draws) {
            OpponentStats stats = opponentStatsMap.computeIfAbsent(opponentKey, key -> new OpponentStats(opponentKey));
            stats.matchCount += matchInc;
            stats.winCount += wins;
            stats.loseCount += loses;
            stats.drawCount += draws;
        }
    }

    private static class OpponentStats {
        private final String opponentKey;

        private int matchCount;
        private int winCount;
        private int loseCount;
        private int drawCount;

        private OpponentStats(String opponentKey) {
            this.opponentKey = opponentKey;
        }
    }
}
