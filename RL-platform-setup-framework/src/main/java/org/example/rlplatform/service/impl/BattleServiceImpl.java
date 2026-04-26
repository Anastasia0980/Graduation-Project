package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.BattleModelSubmissionRepository;
import org.example.rlplatform.Repository.BattleParticipantRepository;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.Repository.UserRepository;
import org.example.rlplatform.Repository.TeamGroupRepository;
import org.example.rlplatform.Repository.TeamMemberRepository;
import org.example.rlplatform.battle.BattleRunner;
import org.example.rlplatform.entity.*;
import org.example.rlplatform.service.BattleService;
import org.example.rlplatform.service.SystemBotService;
import org.example.rlplatform.service.BattleEnvironmentService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.example.rlplatform.vo.BattleModelOptionVO;
import org.example.rlplatform.vo.BattleTaskDetailVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class BattleServiceImpl implements BattleService {

    private static final int DEFAULT_GAMES = 30;
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EvaluationRepository evaluationRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final BattleParticipantRepository battleParticipantRepository;
    private final BattleModelSubmissionRepository battleModelSubmissionRepository;
    private final BattleRunner battleRunner;
    private final ExperimentAssignmentRepository experimentAssignmentRepository;
    private final SystemBotService systemBotService;
    private final UserRepository userRepository;
    private final TeamGroupRepository teamGroupRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final BattleEnvironmentService battleEnvironmentService;

    @Value("${evaluation.workspace:}")
    private String workspace;

    public BattleServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationResultRepository evaluationResultRepository,
            BattleParticipantRepository battleParticipantRepository,
            BattleModelSubmissionRepository battleModelSubmissionRepository,
            BattleRunner battleRunner,
            ExperimentAssignmentRepository experimentAssignmentRepository,
            SystemBotService systemBotService,
            UserRepository userRepository,
            TeamGroupRepository teamGroupRepository,
            TeamMemberRepository teamMemberRepository,
            BattleEnvironmentService battleEnvironmentService
    ) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationResultRepository = evaluationResultRepository;
        this.battleParticipantRepository = battleParticipantRepository;
        this.battleModelSubmissionRepository = battleModelSubmissionRepository;
        this.battleRunner = battleRunner;
        this.experimentAssignmentRepository = experimentAssignmentRepository;
        this.systemBotService = systemBotService;
        this.userRepository = userRepository;
        this.teamGroupRepository = teamGroupRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.battleEnvironmentService = battleEnvironmentService;
    }

    @Override
    public Result<?> submitAndMaybeStart(Integer assignmentId, MultipartFile model, MultipartFile config) {
        return submitBattleModel(assignmentId, model, config);
    }

    @Override
    public Result<?> submitBattleModel(Integer assignmentId, MultipartFile model, MultipartFile config) {
        try {
            if (assignmentId == null) {
                return Result.error("assignmentId is required");
            }
            if (model == null || model.isEmpty()) {
                return Result.error("model is required");
            }
            if (config == null || config.isEmpty()) {
                return Result.error("config is required");
            }

            Long studentId = currentStudentId();
            if (studentId == null) {
                return Result.error("当前登录信息无效，请重新登录");
            }

            ExperimentAssignment assignment = validBattleAssignment(assignmentId);
            if (assignment == null) {
                return Result.error("任务不存在");
            }
            validateTeamCaptainIfNeeded(assignment, studentId.intValue());
            validateTeamLockedIfNeeded(assignment, studentId.intValue());

            String environment = assignment.getEnvironment();
            if (environment == null || environment.isBlank()) {
                return Result.error("任务未配置环境");
            }

            String rel = saveStudentFiles(studentId, assignment.getEnvironment(), model, config);

            BattleModelSubmission submission = new BattleModelSubmission();
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(studentId);
            submission.setEnvironment(environment);
            submission.setGames(DEFAULT_GAMES);
            submission.setStudentDirRel(rel);
            submission.setModelName(safeModelName(model));
            submission.setActive(true);
            submission.setCreateTime(LocalDateTime.now());
            battleModelSubmissionRepository.save(submission);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("submissionId", submission.getId());
            data.put("modelName", submission.getModelName());
            data.put("message", "模型已保存，可在“已提交模型”中选择该模型发起挑战。");
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public List<BattleModelOptionVO> listMyBattleModels(Integer assignmentId) {
        Long studentId = currentStudentId();
        if (studentId == null || assignmentId == null) {
            return Collections.emptyList();
        }
        ExperimentAssignment assignment = validBattleAssignment(assignmentId);
        Long ownerStudentId = resolveSubmissionOwnerStudentId(assignment, studentId);
        List<BattleModelSubmission> submissions = battleModelSubmissionRepository
                .findByAssignmentIdAndStudentIdAndActiveTrueOrderByIdDesc(assignmentId, ownerStudentId);
        return buildBattleModelOptions(assignment, submissions);
    }

    @Override
    public List<BattleModelOptionVO> listOpponentBattleModels(Integer assignmentId, Long mySubmissionId) {
        Long studentId = currentStudentId();
        if (studentId == null || assignmentId == null || mySubmissionId == null) {
            return Collections.emptyList();
        }

        ExperimentAssignment assignment = validBattleAssignment(assignmentId);
        validateTeamCaptainIfNeeded(assignment, studentId.intValue());
        validateTeamLockedIfNeeded(assignment, studentId.intValue());

        BattleModelSubmission mySubmission = battleModelSubmissionRepository.findByIdAndActiveTrue(mySubmissionId)
                .orElseThrow(() -> new RuntimeException("我的模型记录不存在"));

        if (!Objects.equals(mySubmission.getAssignmentId(), assignmentId)) {
            throw new RuntimeException("模型与当前任务不匹配");
        }

        Long ownerStudentId = resolveSubmissionOwnerStudentId(assignment, studentId);
        if (!Objects.equals(mySubmission.getStudentId(), ownerStudentId)) {
            throw new RuntimeException("不能使用他人的模型作为自己的出战模型");
        }

        List<BattleModelSubmission> submissions = battleModelSubmissionRepository
                .findByAssignmentIdAndStudentIdNotAndActiveTrueOrderByIdDesc(assignmentId, ownerStudentId);
        return buildBattleModelOptions(assignment, submissions);
    }

    @Override
    public Result<?> challengeBySubmission(Integer assignmentId, Long mySubmissionId, Long opponentSubmissionId) {
        try {
            if (assignmentId == null) {
                return Result.error("assignmentId is required");
            }
            if (mySubmissionId == null || opponentSubmissionId == null) {
                return Result.error("mySubmissionId and opponentSubmissionId are required");
            }

            Long studentId = currentStudentId();
            if (studentId == null) {
                return Result.error("当前登录信息无效，请重新登录");
            }

            ExperimentAssignment assignment = validBattleAssignment(assignmentId);
            if (assignment == null) {
                return Result.error("任务不存在");
            }
            validateTeamCaptainIfNeeded(assignment, studentId.intValue());
            validateTeamLockedIfNeeded(assignment, studentId.intValue());

            BattleModelSubmission mySubmission = battleModelSubmissionRepository.findByIdAndActiveTrue(mySubmissionId)
                    .orElseThrow(() -> new RuntimeException("我的模型记录不存在"));
            BattleModelSubmission opponentSubmission = battleModelSubmissionRepository.findByIdAndActiveTrue(opponentSubmissionId)
                    .orElseThrow(() -> new RuntimeException("对手模型记录不存在"));

            if (!Objects.equals(mySubmission.getAssignmentId(), assignmentId)
                    || !Objects.equals(opponentSubmission.getAssignmentId(), assignmentId)) {
                return Result.error("模型与当前任务不匹配");
            }

            Long ownerStudentId = resolveSubmissionOwnerStudentId(assignment, studentId);
            if (!Objects.equals(mySubmission.getStudentId(), ownerStudentId)) {
                return Result.error("只能使用当前队伍有效模型发起挑战");
            }

            if (Objects.equals(mySubmission.getStudentId(), opponentSubmission.getStudentId())) {
                return Result.error("不能挑战自己上传的模型");
            }

            Evaluation evaluation = new Evaluation();
            evaluation.setStudentId(studentId.intValue());
            evaluation.setAgentName("BATTLE");
            evaluation.setEnvironment(assignment.getEnvironment());
            evaluation.setModelId(null);
            evaluation.setAssignmentId(assignmentId);
            evaluation.setEpisodes(DEFAULT_GAMES);
            evaluation.setStatus(EvaluationStatus.PENDING);
            evaluation.setCreateTime(LocalDateTime.now());
            evaluation.setUpdateTime(LocalDateTime.now());
            evaluationRepository.save(evaluation);

            BattleParticipant participant = new BattleParticipant();
            participant.setEvaluationId(evaluation.getId());
            participant.setStudent1Id(mySubmission.getStudentId());
            participant.setStudent2Id(opponentSubmission.getStudentId());
            participant.setStudent1SubmissionId(mySubmission.getId());
            participant.setStudent2SubmissionId(opponentSubmission.getId());
            participant.setStudent1DirRel(mySubmission.getStudentDirRel());
            participant.setStudent2DirRel(opponentSubmission.getStudentDirRel());
            participant.setOpponentType("HUMAN");
            participant.setOpponentName(null);
            participant.setDifficulty(null);
            participant.setStudent1ModelName(mySubmission.getModelName());
            participant.setStudent2ModelName(opponentSubmission.getModelName());
            battleParticipantRepository.save(participant);

            battleRunner.runBattleAsync(evaluation.getId());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("evaluationId", evaluation.getId());
            data.put("mySubmissionId", mySubmissionId);
            data.put("opponentSubmissionId", opponentSubmissionId);
            data.put("message", "挑战已发起，平台正在异步执行对战评测。");
            return Result.success(data);
        } catch (Exception e) {
            log.error("submitAndMaybeStart failed assignmentId={}", assignmentId, e);
            return Result.error(e.getMessage());
        }
    }

    @Override
    public Result<?> submitBotAndStart(Integer assignmentId, String difficulty, MultipartFile model, MultipartFile config) {
        try {
            if (assignmentId == null) {
                return Result.error("assignmentId is required");
            }
            if (difficulty == null || difficulty.isBlank()) {
                return Result.error("difficulty is required");
            }
            if (model == null || model.isEmpty()) {
                return Result.error("model is required");
            }
            if (config == null || config.isEmpty()) {
                return Result.error("config is required");
            }

            Long studentId = currentStudentId();
            if (studentId == null) {
                return Result.error("当前登录信息无效，请重新登录");
            }

            ExperimentAssignment assignment = validBattleAssignment(assignmentId);
            if (assignment == null) {
                return Result.error("任务不存在");
            }

            String environment = assignment.getEnvironment();
            if (environment == null || environment.isBlank()) {
                return Result.error("任务未配置环境");
            }

            String rel = saveStudentFiles(studentId, assignment.getEnvironment(), model, config);

            String botAbsDir = systemBotService.getBotAbsoluteDir(assignmentId, difficulty);
            Path botPath = Paths.get(botAbsDir);
            if (!Files.exists(botPath.resolve("config.json")) || !Files.exists(botPath.resolve("model.pt"))) {
                return Result.error("该难度人机模型尚未配置");
            }

            Evaluation evaluation = new Evaluation();
            evaluation.setStudentId(studentId.intValue());
            evaluation.setAgentName("BATTLE_BOT");
            evaluation.setEnvironment(environment);
            evaluation.setModelId(null);
            evaluation.setAssignmentId(assignmentId);
            evaluation.setEpisodes(DEFAULT_GAMES);
            evaluation.setStatus(EvaluationStatus.PENDING);
            evaluation.setCreateTime(LocalDateTime.now());
            evaluation.setUpdateTime(LocalDateTime.now());
            evaluationRepository.save(evaluation);

            BattleParticipant participant = new BattleParticipant();
            participant.setEvaluationId(evaluation.getId());
            participant.setStudent1Id(studentId);
            participant.setStudent2Id(null);
            participant.setStudent1SubmissionId(null);
            participant.setStudent2SubmissionId(null);
            participant.setStudent1DirRel(rel);
            participant.setStudent2DirRel(botAbsDir);
            participant.setOpponentType("BOT");
            participant.setOpponentName(mapDifficultyName(difficulty));
            participant.setDifficulty(difficulty.toLowerCase(Locale.ROOT));
            participant.setStudent1ModelName(safeModelName(model));
            participant.setStudent2ModelName("系统模型");
            battleParticipantRepository.save(participant);

            battleRunner.runBattleAsync(evaluation.getId());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("queued", false);
            data.put("matched", true);
            data.put("evaluationId", evaluation.getId());
            data.put("assignmentId", assignmentId);
            data.put("environment", environment);
            data.put("games", DEFAULT_GAMES);
            data.put("message", "人机对战已启动。");
            return Result.success(data);

        } catch (Exception e) {
            log.error("submitBotAndStart failed assignmentId={}", assignmentId, e);
            return Result.error(e.getMessage());
        }
    }

    @Override
    public List<BattleTaskDetailVO> listBattleTasks() {
        List<Evaluation> evaluations = new ArrayList<>();
        evaluations.addAll(evaluationRepository.findByAgentNameOrderByIdDesc("BATTLE"));
        evaluations.addAll(evaluationRepository.findByAgentNameOrderByIdDesc("BATTLE_BOT"));
        evaluations.sort((a, b) -> Long.compare(b.getId(), a.getId()));

        List<BattleTaskDetailVO> result = new ArrayList<>();

        for (Evaluation evaluation : evaluations) {
            BattleTaskDetailVO vo = new BattleTaskDetailVO();
            vo.setEvaluationId(evaluation.getId());
            vo.setStatus(buildStatusText(evaluation.getStatus()));

            Optional<BattleParticipant> participantOpt = battleParticipantRepository.findByEvaluationId(evaluation.getId());
            if (participantOpt.isPresent()) {
                BattleParticipant participant = participantOpt.get();
                vo.setStudent1Id(participant.getStudent1Id());
                vo.setStudent2Id(participant.getStudent2Id());
            }

            List<EvaluationResult> evaluationResults = evaluationResultRepository.findByEvaluationId(evaluation.getId());
            if (evaluationResults != null && !evaluationResults.isEmpty()) {
                EvaluationResult er = evaluationResults.get(0);
                vo.setWinner(er.getWinner());
                vo.setResultDir(er.getResultDir());
                vo.setWinnerText("已出结果");
            } else {
                vo.setWinner(null);
                vo.setWinnerText(buildStatusText(evaluation.getStatus()));
            }

            result.add(vo);
        }

        return result;
    }

    private ExperimentAssignment validBattleAssignment(Integer assignmentId) {
        ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        if (assignment == null) {
            return null;
        }
        if (assignment.getEvaluationMode() != EvaluationMode.VERSUS
                && assignment.getEvaluationMode() != EvaluationMode.TEAM) {
            throw new RuntimeException("当前任务不是对战模式");
        }
        return assignment;
    }


    private void validateTeamCaptainIfNeeded(ExperimentAssignment assignment, Integer currentUserId) {
        if (assignment == null || assignment.getEvaluationMode() != EvaluationMode.TEAM) {
            return;
        }
        var teamOpt = teamGroupRepository.findByAssignmentIdAndCaptainStudentIdAndIsDeletedFalse(assignment.getId(), currentUserId);
        if (teamOpt.isEmpty()) {
            throw new RuntimeException("当前任务为分组对战，仅队长可提交模型和发起挑战");
        }
    }


    private void validateTeamLockedIfNeeded(ExperimentAssignment assignment, Integer currentUserId) {
        if (assignment == null || assignment.getEvaluationMode() != EvaluationMode.TEAM) {
            return;
        }
        TeamGroup team = findMyTeam(assignment.getId(), currentUserId);
        if (team == null) {
            throw new RuntimeException("当前未加入队伍");
        }
        if (!Boolean.TRUE.equals(team.getLocked())) {
            throw new RuntimeException("请先确定队伍成员或等待自由组队截止后自动锁定");
        }
    }

    private Long resolveSubmissionOwnerStudentId(ExperimentAssignment assignment, Long currentStudentId) {
        if (assignment == null || assignment.getEvaluationMode() != EvaluationMode.TEAM) {
            return currentStudentId;
        }
        TeamGroup team = findMyTeam(assignment.getId(), currentStudentId.intValue());
        if (team == null) {
            throw new RuntimeException("当前未加入队伍");
        }
        return team.getCaptainStudentId().longValue();
    }

    private TeamGroup findMyTeam(Integer assignmentId, Integer studentId) {
        ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        List<TeamMember> memberships = teamMemberRepository.findByStudentIdAndIsDeletedFalse(studentId);
        for (TeamMember member : memberships) {
            TeamGroup team = teamGroupRepository.findByIdAndIsDeletedFalse(member.getTeamId());
            if (team != null && Objects.equals(team.getAssignmentId(), assignmentId)) {
                return ensureTeamAutoLocked(team, assignment);
            }
        }
        return null;
    }

    private TeamGroup ensureTeamAutoLocked(TeamGroup team, ExperimentAssignment assignment) {
        if (team == null || assignment == null || Boolean.TRUE.equals(team.getLocked())) {
            return team;
        }
        LocalDateTime teamGroupDeadline = assignment.getTeamGroupDeadline();
        if (teamGroupDeadline != null && !LocalDateTime.now().isBefore(teamGroupDeadline)) {
            team.setLocked(true);
            team.setLockTime(LocalDateTime.now());
            team.setUpdateTime(LocalDateTime.now());
            team = teamGroupRepository.save(team);
        }
        return team;
    }

    private Long currentStudentId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || claims.get("id") == null) {
            return null;
        }
        Integer currentUserId = (Integer) claims.get("id");
        return currentUserId.longValue();
    }

    private String saveStudentFiles(Long studentId, String environmentCode, MultipartFile model, MultipartFile config) throws Exception {
        String resolvedWorkspace = battleEnvironmentService.resolveWorkspace(environmentCode);
        String baseDir = (resolvedWorkspace != null && !resolvedWorkspace.isBlank())
                ? resolvedWorkspace
                : ((workspace != null && !workspace.isBlank())
                ? workspace
                : Paths.get(System.getProperty("user.dir")).toString());

        String uuid = UUID.randomUUID().toString().replace("-", "");
        Path studentDir = Paths.get(baseDir, "uploads", String.valueOf(studentId), uuid);
        Files.createDirectories(studentDir);

        Path configPath = studentDir.resolve("config.json");
        Path modelPath = studentDir.resolve("model.pt");

        config.transferTo(configPath);
        model.transferTo(modelPath);

        return Paths.get("uploads", String.valueOf(studentId), uuid)
                .toString()
                .replace("\\", "/");
    }

    private String safeModelName(MultipartFile model) {
        String name = model == null ? null : model.getOriginalFilename();
        return (name == null || name.isBlank()) ? "model.pt" : name;
    }

    private List<BattleModelOptionVO> buildBattleModelOptions(ExperimentAssignment assignment, List<BattleModelSubmission> submissions) {
        List<BattleModelOptionVO> result = new ArrayList<>();
        for (BattleModelSubmission submission : submissions) {
            BattleModelOptionVO vo = new BattleModelOptionVO();
            vo.setSubmissionId(submission.getId());
            vo.setStudentId(submission.getStudentId());

            String studentName = "未知学生";
            if (submission.getStudentId() != null) {
                Optional<User> userOpt = userRepository.findById(submission.getStudentId().intValue());
                if (userOpt.isPresent() && userOpt.get().getUsername() != null && !userOpt.get().getUsername().isBlank()) {
                    studentName = userOpt.get().getUsername();
                }
            }

            if (assignment != null && assignment.getEvaluationMode() == EvaluationMode.TEAM && submission.getStudentId() != null) {
                var teamOpt = teamGroupRepository.findByAssignmentIdAndCaptainStudentIdAndIsDeletedFalse(
                        assignment.getId(),
                        submission.getStudentId().intValue()
                );
                if (teamOpt.isPresent()) {
                    studentName = buildTeamDisplayName(teamOpt.get());
                }
            }

            vo.setStudentName(studentName);
            vo.setModelName(submission.getModelName());
            vo.setSubmitTime(submission.getCreateTime() == null ? "--" : submission.getCreateTime().format(DATETIME_FORMATTER));

            int[] record = countBattleRecord(submission.getId());
            vo.setWinCount(record[0]);
            vo.setLoseCount(record[1]);
            vo.setDrawCount(record[2]);

            result.add(vo);
        }
        return result;
    }

    private String buildTeamDisplayName(TeamGroup team) {
        if (team == null) {
            return "未知队伍";
        }
        List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsDeletedFalseOrderByIdAsc(team.getId());
        List<Integer> memberIds = new ArrayList<>();
        for (TeamMember member : members) {
            memberIds.add(member.getStudentId());
        }

        Map<Integer, String> nameMap = new HashMap<>();
        if (!memberIds.isEmpty()) {
            for (User user : userRepository.findAllById(memberIds)) {
                String name = user.getUsername();
                if (name == null || name.isBlank()) {
                    name = user.getNickname();
                }
                nameMap.put(user.getId(), name == null ? "未知学生" : name);
            }
        }

        String captainName = nameMap.getOrDefault(team.getCaptainStudentId(), "未知学生");
        List<String> parts = new ArrayList<>();
        parts.add(team.getTeamName());
        parts.add(captainName);
        for (TeamMember member : members) {
            if (Objects.equals(member.getStudentId(), team.getCaptainStudentId())) {
                continue;
            }
            parts.add(nameMap.getOrDefault(member.getStudentId(), "未知学生"));
        }
        return String.join(" - ", parts);
    }

    private int[] countBattleRecord(Long submissionId) {
        int win = 0;
        int lose = 0;
        int draw = 0;

        List<BattleParticipant> participants = battleParticipantRepository
                .findByStudent1SubmissionIdOrStudent2SubmissionIdOrderByIdDesc(submissionId, submissionId);

        for (BattleParticipant participant : participants) {
            if (participant.getEvaluationId() == null) {
                continue;
            }
            List<EvaluationResult> results = evaluationResultRepository.findByEvaluationId(participant.getEvaluationId());
            if (results == null || results.isEmpty()) {
                continue;
            }
            EvaluationResult result = results.get(0);
            if (result.getWinner() == null) {
                continue;
            }
            if (result.getWinner() == 0) {
                draw++;
                continue;
            }
            boolean isStudent1 = Objects.equals(participant.getStudent1SubmissionId(), submissionId);
            if (isStudent1) {
                if (result.getWinner() == 1) {
                    win++;
                } else if (result.getWinner() == 2) {
                    lose++;
                }
            } else {
                if (result.getWinner() == 2) {
                    win++;
                } else if (result.getWinner() == 1) {
                    lose++;
                }
            }
        }

        return new int[]{win, lose, draw};
    }

    private String buildStatusText(EvaluationStatus status) {
        if (status == null) return "未知状态";
        return switch (status) {
            case PENDING -> "排队中";
            case RUNNING -> "测评中";
            case FAILED -> "测评失败";
            case FINISHED -> "已完成";
        };
    }

    private String mapDifficultyName(String difficulty) {
        String d = difficulty == null ? "" : difficulty.toLowerCase(Locale.ROOT);
        return switch (d) {
            case "easy" -> "简单人机";
            case "medium" -> "中等人机";
            case "hard" -> "困难人机";
            default -> "人机";
        };
    }
}