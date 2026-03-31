package org.example.rlplatform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rlplatform.Repository.BattleParticipantRepository;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.Repository.ModelFileRepository;
import org.example.rlplatform.entity.*;
import org.example.rlplatform.service.EvaluationResultService;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.List;

@Service
public class EvaluationResultServiceImpl implements EvaluationResultService {

    @Autowired
    private EvaluationResultRepository evaluationResultRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private BattleParticipantRepository battleParticipantRepository;

    @Autowired
    private ExperimentAssignmentRepository experimentAssignmentRepository;

    @Autowired
    private ModelFileRepository modelFileRepository;

    @Autowired
    private UserService userService;

    @Value("${evaluation.workspace:}")
    private String workspace;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void create(EvaluationResult evaluationResult) {
        evaluationResultRepository.save(evaluationResult);
    }

    @Override
    public EvaluationResult getById(Long id) {
        return evaluationResultRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @Override
    public List<EvaluationResult> listByEvaluationId(Long evaluationId) {
        return evaluationResultRepository.findByEvaluationId(evaluationId);
    }

    @Override
    public List<EvaluationResult> list() {
        return evaluationResultRepository.findAll();
    }

    @Override
    public Resource getVideo(Long id) {
        EvaluationResult er = getById(id);
        if (er.getResultDir() == null || er.getResultDir().isBlank()) {
            throw new IllegalArgumentException("no result_dir for evaluation result id: " + id);
        }
        String base = getBaseDir();
        Path videoPath = Paths.get(base, er.getResultDir() + ".mp4");
        File file = videoPath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("video file not found: " + videoPath);
        }
        return new FileSystemResource(file);
    }

    @Override
    public Resource getLog(Long id) {
        EvaluationResult er = getById(id);
        Evaluation evaluation = evaluationRepository.findById(er.getEvaluationId()).orElseThrow(RuntimeException::new);
        Optional<BattleParticipant> participantOpt = battleParticipantRepository.findByEvaluationId(evaluation.getId());
        ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(evaluation.getAssignmentId());

        StringBuilder sb = new StringBuilder();
        sb.append("任务名称：").append(assignment != null ? assignment.getTitle() : "未知任务").append("\n");
        sb.append("测评环境：").append(defaultText(evaluation.getEnvironment())).append("\n");
        sb.append("提交时间：").append(evaluation.getCreateTime() == null ? "--" : evaluation.getCreateTime().format(DATETIME_FORMATTER)).append("\n");
        sb.append("更新时间：").append(evaluation.getUpdateTime() == null ? "--" : evaluation.getUpdateTime().format(DATETIME_FORMATTER)).append("\n");
        sb.append("测评状态：").append(mapStatus(evaluation.getStatus())).append("\n");

        if (participantOpt.isPresent()) {
            BattleParticipant participant = participantOpt.get();
            Long currentStudentId = currentStudentId();
            if (currentStudentId == null) {
                currentStudentId = evaluation.getStudentId() == null ? null : evaluation.getStudentId().longValue();
            }

            sb.append("任务类型：")
                    .append("BOT".equalsIgnoreCase(participant.getOpponentType()) ? "人机对战" : "学生对战")
                    .append("\n");

            String myModelName = resolveMyBattleModelName(participant, currentStudentId);
            String opponentName = resolveOpponentName(participant, currentStudentId);
            String opponentModelName = resolveOpponentModelName(participant, currentStudentId);

            sb.append("我的模型：").append(defaultText(myModelName)).append("\n");
            sb.append("对手：").append(defaultText(opponentName)).append("\n");
            sb.append("对手模型：").append(defaultText(opponentModelName)).append("\n");

            JsonNode root = parseJson(er.getDetailedResults());
            if (root != null) {
                int games = root.path("games").asInt(evaluation.getEpisodes() == null ? 0 : evaluation.getEpisodes());
                int win1 = root.path("win1").asInt(0);
                int win2 = root.path("win2").asInt(0);
                int draw = root.path("draw").asInt(0);
                double winRate1 = root.path("winRate1").asDouble(0.0);
                double winRate2 = root.path("winRate2").asDouble(0.0);

                boolean isStudent2 = currentStudentId != null
                        && participant.getStudent2Id() != null
                        && participant.getStudent2Id().equals(currentStudentId);

                int myWin = isStudent2 ? win2 : win1;
                int myLose = isStudent2 ? win1 : win2;
                double myWinRate = isStudent2 ? winRate2 : winRate1;
                double opponentWinRate = isStudent2 ? winRate1 : winRate2;

                sb.append("对战轮数：").append(games).append("\n");
                sb.append("\n");
                sb.append("结果摘要：").append("\n");
                sb.append("我方胜：").append(myWin).append("\n");
                sb.append("我方负：").append(myLose).append("\n");
                sb.append("平局：").append(draw).append("\n");
                sb.append("我方胜率：").append(String.format("%.1f%%", myWinRate * 100)).append("\n");
                sb.append("对方胜率：").append(String.format("%.1f%%", opponentWinRate * 100)).append("\n");
                sb.append("最终结果：").append(buildBattleFinalResult(er, participant, currentStudentId)).append("\n");
            }

        } else {
            sb.append("任务类型：单人测评").append("\n");

            ModelFile modelFile = evaluation.getModelId() == null ? null : modelFileRepository.findById(evaluation.getModelId());
            sb.append("提交模型：").append(modelFile != null ? defaultText(modelFile.getFileName()) : "--").append("\n");

            JsonNode root = parseJson(er.getDetailedResults());
            if (root != null) {
                int rounds = root.has("student_rewards") && root.path("student_rewards").isArray()
                        ? root.path("student_rewards").size()
                        : (evaluation.getEpisodes() == null ? 0 : evaluation.getEpisodes());

                double studentAvgReward = root.path("student_avg_reward").asDouble(0.0);
                sb.append("测评轮数：").append(rounds).append("\n");
                sb.append("平均奖励：").append(String.format("%.4f", studentAvgReward)).append("\n");

                if (root.has("student_rewards") && root.path("student_rewards").isArray()) {
                    sb.append("各轮奖励：").append(root.path("student_rewards").toString()).append("\n");
                }

                boolean hasBaseline = root.has("baseline_rewards")
                        && root.path("baseline_rewards").isArray()
                        && root.path("baseline_rewards").size() > 0;

                if (hasBaseline) {
                    double baselineAvgReward = root.path("baseline_avg_reward").asDouble(0.0);
                    sb.append("基线平均奖励：").append(String.format("%.4f", baselineAvgReward)).append("\n");
                    Integer winner = root.has("winner") ? root.path("winner").asInt() : null;
                    if (winner != null) {
                        sb.append("对比结果：").append(winner == 1 ? "学生模型优于基线" : "基线优于学生模型").append("\n");
                    }
                }
            }
        }

        if (evaluation.getErrorMessage() != null && !evaluation.getErrorMessage().isBlank()) {
            sb.append("\n");
            sb.append("错误信息：").append("\n");
            sb.append(evaluation.getErrorMessage()).append("\n");
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return new ByteArrayResource(bytes);
    }

    @Override
    public Resource downloadModelPackage(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId).orElseThrow(RuntimeException::new);
        String base = getBaseDir();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8);

            Optional<BattleParticipant> participantOpt = battleParticipantRepository.findByEvaluationId(evaluationId);
            if (participantOpt.isPresent()) {
                BattleParticipant participant = participantOpt.get();
                Long currentStudentId = currentStudentId();
                if (currentStudentId == null) {
                    currentStudentId = evaluation.getStudentId() == null ? null : evaluation.getStudentId().longValue();
                }

                String relDir = resolveMyBattleDir(participant, currentStudentId);
                if (relDir == null || relDir.isBlank()) {
                    throw new IllegalArgumentException("未找到当前模型目录");
                }

                Path modelPath = Paths.get(base, relDir, "model.pt");
                Path configPath = Paths.get(base, relDir, "config.json");

                addFileToZip(zos, modelPath, "model.pt");
                addFileToZip(zos, configPath, "config.json");
            } else {
                if (evaluation.getModelId() == null) {
                    throw new IllegalArgumentException("未找到模型记录");
                }

                ModelFile modelFile = modelFileRepository.findById(evaluation.getModelId());
                if (modelFile == null) {
                    throw new IllegalArgumentException("模型记录不存在");
                }

                Path modelPath = Paths.get(base, modelFile.getFilePath());
                Path configPath = modelFile.getConfigPath() == null
                        ? null
                        : Paths.get(base, modelFile.getConfigPath());

                String modelEntryName = modelFile.getFileName() == null || modelFile.getFileName().isBlank()
                        ? "model.bin"
                        : modelFile.getFileName();

                addFileToZip(zos, modelPath, modelEntryName);
                if (configPath != null) {
                    addFileToZip(zos, configPath, "config.json");
                }
            }

            zos.finish();
            zos.close();
            return new ByteArrayResource(baos.toByteArray());
        } catch (Exception e) {
            throw new IllegalArgumentException("模型下载失败：" + e.getMessage(), e);
        }
    }

    private String getBaseDir() {
        return (workspace != null && !workspace.isBlank())
                ? workspace
                : Paths.get(System.getProperty("user.dir")).toString();
    }

    private Long currentStudentId() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims == null || claims.get("id") == null) {
                return null;
            }
            Integer currentUserId = (Integer) claims.get("id");
            return currentUserId.longValue();
        } catch (Exception e) {
            return null;
        }
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

    private String mapStatus(EvaluationStatus status) {
        if (status == null) return "未知状态";
        return switch (status) {
            case PENDING -> "待开始";
            case RUNNING -> "测评中";
            case FINISHED -> "已完成";
            case FAILED -> "测评失败";
        };
    }

    private String resolveOpponentName(BattleParticipant participant, Long currentStudentId) {
        if ("BOT".equalsIgnoreCase(participant.getOpponentType())) {
            return participant.getOpponentName() == null ? "人机" : participant.getOpponentName();
        }

        if (currentStudentId != null && participant.getStudent1Id() != null && participant.getStudent1Id().equals(currentStudentId)) {
            if (participant.getStudent2Id() == null) return "真人对手";
            User user = userService.findByIdAndIsDeletedFalse(participant.getStudent2Id().intValue());
            return user != null ? user.getUsername() : "真人对手";
        }

        if (currentStudentId != null && participant.getStudent2Id() != null && participant.getStudent2Id().equals(currentStudentId)) {
            if (participant.getStudent1Id() == null) return "真人对手";
            User user = userService.findByIdAndIsDeletedFalse(participant.getStudent1Id().intValue());
            return user != null ? user.getUsername() : "真人对手";
        }

        return "真人对手";
    }

    private String resolveMyBattleModelName(BattleParticipant participant, Long currentStudentId) {
        if (currentStudentId != null && participant.getStudent2Id() != null && participant.getStudent2Id().equals(currentStudentId)) {
            return participant.getStudent2ModelName();
        }
        return participant.getStudent1ModelName();
    }

    private String resolveOpponentModelName(BattleParticipant participant, Long currentStudentId) {
        if ("BOT".equalsIgnoreCase(participant.getOpponentType())) {
            return participant.getStudent2ModelName() == null ? "系统模型" : participant.getStudent2ModelName();
        }
        if (currentStudentId != null && participant.getStudent2Id() != null && participant.getStudent2Id().equals(currentStudentId)) {
            return participant.getStudent1ModelName();
        }
        return participant.getStudent2ModelName();
    }

    private String resolveMyBattleDir(BattleParticipant participant, Long currentStudentId) {
        if (currentStudentId != null && participant.getStudent2Id() != null && participant.getStudent2Id().equals(currentStudentId)) {
            return participant.getStudent2DirRel();
        }
        return participant.getStudent1DirRel();
    }

    private String buildBattleFinalResult(EvaluationResult er, BattleParticipant participant, Long currentStudentId) {
        if (er.getWinner() == null) {
            return "已完成";
        }
        if (er.getWinner() == 0) {
            return "平局";
        }

        boolean isStudent2 = currentStudentId != null
                && participant.getStudent2Id() != null
                && participant.getStudent2Id().equals(currentStudentId);

        if (!isStudent2) {
            return er.getWinner() == 1 ? "获胜" : "失败";
        }
        return er.getWinner() == 2 ? "获胜" : "失败";
    }

    private void addFileToZip(ZipOutputStream zos, Path filePath, String entryName) throws Exception {
        if (filePath == null || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("文件不存在：" + filePath);
        }
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(Files.readAllBytes(filePath));
        zos.closeEntry();
    }

    private String defaultText(String text) {
        return (text == null || text.isBlank()) ? "--" : text;
    }
}