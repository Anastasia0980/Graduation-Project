package org.example.rlplatform.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.EvaluationResult;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.entity.EvaluationStatus;
import org.example.rlplatform.service.CurriculumProgressService;
import org.example.rlplatform.service.ModelFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class EvaluationExecuter {

    @Value("${evaluation.python:python}")
    private String pythonCmd;

    @Value("${evaluation.script:scripts/evaluate.py}")
    private String scriptPath;

    @Value("${evaluation.workspace:}")
    private String workspaceConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EvaluationResultRepository evaluationResultRepository;

    @Autowired
    private ModelFileService modelFileService;

    @Autowired
    private CurriculumProgressService curriculumProgressService;

    public void execute(Evaluation evaluation) {
        final Long evalId = evaluation.getId();
        log.info("Evaluation start id={} env={} episodes={} modelId={} taskId={}",
                evalId, evaluation.getEnvironment(), evaluation.getEpisodes(), evaluation.getModelId(),
                evaluation.getTaskId());

        String modelName = null;
        String configPath = null;
        if (evaluation.getModelId() != null) {
            var modelFile = modelFileService.getById(evaluation.getModelId());
            if (modelFile != null) {
                modelName = modelFile.getFileName();
                configPath = workspaceConfig + "/" + modelFile.getConfigPath();
            }
        }

        if (configPath != null && !configPath.isBlank()) {
            if (!Paths.get(configPath.replace("\\", "/")).toFile().exists()) {
                log.warn("Evaluation id={} aborted: config not found path={}", evalId, configPath);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("Config file not found: " + configPath);
                saveEvaluationResult(evaluation, null, null);
                return;
            }
        }

        Path baselineModelPath = null;
        String agentType = null;
        String baselineAgentType = null;
        try {
            JsonNode configRoot = objectMapper.readTree(Paths.get(configPath).toFile());
            agentType = configRoot.path("algorithm").asText(null);
            if (agentType == null || agentType.isBlank()) {
                log.warn("Evaluation id={} aborted: algorithm missing in config path={}", evalId, configPath);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("algorithm not found in config: " + configPath);
                saveEvaluationResult(evaluation, null, null);
                return;
            }

            String baselinePath = evaluation.getBaselineModelPath();
            if (baselinePath == null || baselinePath.isBlank()) {
                log.warn("Evaluation id={} aborted: baseline model path not set", evalId);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("baseline model path not set for evaluation id: " + evaluation.getId());
                saveEvaluationResult(evaluation, null, null);
                return;
            }
            baselineModelPath = Paths.get(baselinePath);
            baselineAgentType = resolveBaselineAgentType(evaluation, baselineModelPath);

        } catch (Exception e) {
            log.warn("Evaluation id={} aborted: failed to read config path={}: {}", evalId, configPath, e.getMessage());
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorMessage("Failed to read config file: " + e.getMessage());
            saveEvaluationResult(evaluation, null, null);
            return;
        }

        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path script = cwd.resolve(scriptPath).normalize();
        if (!script.toFile().exists()) {
            log.warn("Evaluation id={} aborted: script not found path={}", evalId, script);
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorMessage("Script not found: " + script);
            saveEvaluationResult(evaluation, null, null);
            return ;
        }

        String rawTaskId = evaluation.getTaskId();
        if (rawTaskId == null || rawTaskId.isBlank()) {
            log.warn("Evaluation id={} aborted: task_id not set (闯关模式必填)", evalId);
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorMessage("task_id not set for evaluation");
            saveEvaluationResult(evaluation, null, null);
            return;
        }

        String stageSpecRel = evaluation.getStageSpecPath();
        boolean useStageSpec = stageSpecRel != null && !stageSpecRel.isBlank();
        String taskIdForScript;
        if (useStageSpec) {
            String tid = rawTaskId.trim();
            if (!tid.matches("^[a-zA-Z0-9_.-]{1,80}$")) {
                log.warn("Evaluation id={} aborted: invalid stage task_id={}", evalId, rawTaskId);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("invalid task_id for curriculum stage: " + rawTaskId);
                saveEvaluationResult(evaluation, null, null);
                return;
            }
            taskIdForScript = tid;
            evaluation.setTaskId(tid);
        } else {
            String taskIdNorm = rawTaskId.trim().toUpperCase(Locale.ROOT);
            if (!taskIdNorm.matches("T(10|[1-9])")) {
                log.warn("Evaluation id={} aborted: invalid task_id={}", evalId, rawTaskId);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("invalid task_id (expect T1…T10): " + rawTaskId);
                saveEvaluationResult(evaluation, null, null);
                return;
            }
            taskIdForScript = taskIdNorm;
            evaluation.setTaskId(taskIdNorm);
        }

        if (useStageSpec && (workspaceConfig == null || workspaceConfig.isBlank())) {
            log.warn("Evaluation id={} aborted: workspace not set but stage_spec_path present", evalId);
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorMessage("workspace not set for stage_spec_path");
            saveEvaluationResult(evaluation, null, null);
            return;
        }

        Path stageSpecAbs = null;
        if (useStageSpec) {
            Path rel = Paths.get(stageSpecRel.trim());
            stageSpecAbs = rel.isAbsolute()
                    ? rel.normalize()
                    : Paths.get(workspaceConfig.trim()).resolve(rel).normalize();
            if (!stageSpecAbs.toFile().exists()) {
                log.warn("Evaluation id={} aborted: stage_spec not found path={}", evalId, stageSpecAbs);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("stage_spec file not found: " + stageSpecAbs);
                saveEvaluationResult(evaluation, null, null);
                return;
            }
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(pythonCmd);
        cmd.add(script.toString());
        cmd.add("--env");
        cmd.add(evaluation.getEnvironment());
        cmd.add("--agent");
        cmd.add(agentType);
        cmd.add("--model_name");
        cmd.add(modelName);
        cmd.add("--episodes");
        cmd.add(String.valueOf(evaluation.getEpisodes()));
        cmd.add("--workspace");
        cmd.add(workspaceConfig);
        cmd.add("--baseline_model_path");
        cmd.add(baselineModelPath.toString());
        cmd.add("--baseline_agent");
        cmd.add(baselineAgentType);
        cmd.add("--config_path");
        cmd.add(configPath);
        cmd.add("--task_id");
        cmd.add(taskIdForScript);
        if (useStageSpec) {
            cmd.add("--stage_spec_path");
            cmd.add(stageSpecAbs.toString());
        }
        cmd.add("--render_video");

        ProcessBuilder pb = new ProcessBuilder(cmd);

        pb.redirectErrorStream(true);
        pb.directory(cwd.toFile());

        log.info("Evaluation id={} spawning process python={} script={} cwd={} agent={} modelName={} taskId={} stageSpec={}",
                evalId, pythonCmd, script, cwd, agentType, modelName, taskIdForScript, useStageSpec ? stageSpecAbs : "-");

        StringBuilder output = new StringBuilder();
        long processStartMs = System.currentTimeMillis();
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            long elapsedMs = System.currentTimeMillis() - processStartMs;
            if (!finished) {
                process.destroyForcibly();
                log.warn("Evaluation id={} failed: python timeout after {}ms (limit 30m)", evalId, elapsedMs);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("Python execution timeout");
                saveEvaluationResult(evaluation, null, null);
                return;
            }

            int exitCode = process.exitValue();
            String out = output.toString().trim();
            if (log.isDebugEnabled()) {
                log.debug("Evaluation id={} raw output ({} chars): {}", evalId, out.length(),
                        out.length() > 4000 ? out.substring(0, 4000) + "..." : out);
            }

            String jsonLine = extractJsonLine(out);
            JsonNode root = null;
            if (jsonLine != null) {
                try {
                    root = objectMapper.readTree(jsonLine);
                } catch (Exception ignored) {
                    // 解析失败时 root 仍为 null
                }
            }

            if (exitCode != 0) {
                String errMsg = root != null && root.has("error")
                        ? root.path("error").asText()
                        : (out.isEmpty() ? "Python script failed (no output)" : out);
                log.warn("Evaluation id={} python exitCode={} after {}ms error={}", evalId, exitCode, elapsedMs, errMsg);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage(errMsg);
                saveEvaluationResult(evaluation, root, jsonLine);
                return;
            }

            if (root == null) {
                String preview = out.length() > 500 ? out.substring(0, 500) + "..." : out;
                log.warn("Evaluation id={} failed: no valid JSON line in output after {}ms preview={}",
                        evalId, elapsedMs, preview);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("Invalid JSON from script. Output: " +
                        (out.length() > 500 ? out.substring(0, 500) + "..." : out));
                saveEvaluationResult(evaluation, null, jsonLine);
                return;
            }

            String status = root.has("status") ? root.path("status").asText() : EvaluationStatus.FAILED.name();
            evaluation.setStatus(EvaluationStatus.valueOf(status.toUpperCase()));
            if (root.has("error")) {
                evaluation.setErrorMessage(root.path("error").asText());
            }

            if (evaluation.getStatus() == EvaluationStatus.FINISHED) {
                String tid = evaluation.getTaskId();
                if (tid != null && !tid.isBlank()) {
                    if (!root.has("student_avg_reward")) {
                        evaluation.setStatus(EvaluationStatus.FAILED);
                        evaluation.setErrorMessage("脚本未返回 student_avg_reward，无法判定闯关结果");
                    } else if (!root.has("baseline_avg_reward")) {
                        evaluation.setStatus(EvaluationStatus.FAILED);
                        evaluation.setErrorMessage("脚本未返回 baseline_avg_reward，无法判定闯关结果");
                    } else {
                        double studentReward = root.path("student_avg_reward").asDouble(Double.NaN);
                        double baselineReward = root.path("baseline_avg_reward").asDouble(Double.NaN);
                        if (Double.isNaN(studentReward)) {
                            evaluation.setStatus(EvaluationStatus.FAILED);
                            evaluation.setErrorMessage("student_avg_reward 无法解析为数值");
                        } else if (Double.isNaN(baselineReward)) {
                            evaluation.setStatus(EvaluationStatus.FAILED);
                            evaluation.setErrorMessage("baseline_avg_reward 无法解析为数值");
                        } else if (studentReward > baselineReward) {
                            curriculumProgressService.recordPassedStage(
                                    evaluation.getStudentId(),
                                    evaluation.getAssignmentId(),
                                    tid.trim());
                        } else {
                            log.info("Evaluation id={} not advanced: student_avg_reward={} <= baseline_avg_reward={} taskId={}",
                                    evalId, studentReward, baselineReward, tid);
                        }
                    }
                }
            }

            log.info("Evaluation id={} finished status={} in {}ms", evalId, evaluation.getStatus(), elapsedMs);
            saveEvaluationResult(evaluation, root, jsonLine);
        } catch (Exception e) {
            log.error("Evaluation id={} unexpected error", evalId, e);
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorMessage(e.getMessage());
            saveEvaluationResult(evaluation, null, null);
        }
    }


    private void saveEvaluationResult(Evaluation evaluation, JsonNode root, String jsonLine) {
        try {
            EvaluationResult er = new EvaluationResult();
            er.setEvaluationId(evaluation.getId());
            er.setResult(evaluation.getStatus() == EvaluationStatus.FINISHED ? 0 : 1);
//            System.out.println("Saving EvaluationResult..." + root.toString());
            if (jsonLine != null) {
                er.setDetailedResults(jsonLine);
            } else if (root != null) {
                er.setDetailedResults(root.toString());
            }
            if (root != null && root.has("result_dir")) {
                er.setResultDir(root.path("result_dir").asText());
            }
            if (root != null && root.has("winner")) {
                er.setWinner(root.path("winner").asInt());
            }
            String logContent = null;
            if (jsonLine != null && !jsonLine.isBlank()) {
                logContent = jsonLine;
            } else if (root != null) {
                logContent = root.toString();
            }
            writeResultLog(er.getResultDir(), logContent);
            evaluationResultRepository.save(er);
            log.debug("Evaluation id={} result row saved", evaluation.getId());
        } catch (Exception e) {
            log.error("Evaluation id={} save EvaluationResult failed", evaluation.getId(), e);
            evaluation.setStatus(EvaluationStatus.FAILED);
            if (evaluation.getErrorMessage() == null || evaluation.getErrorMessage().isBlank()) {
                evaluation.setErrorMessage("Save result failed: " + e.getMessage());
            }
        }
    }

    private void writeResultLog(String resultDir, String content) throws Exception {
        if (resultDir == null || resultDir.isBlank() || content == null) {
            return;
        }
        String base = (workspaceConfig != null && !workspaceConfig.isBlank())
                ? workspaceConfig
                : Paths.get(System.getProperty("user.dir")).toString();
        Path logPath = Paths.get(base, resultDir + ".log");
        Path parent = logPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                logPath,
                content + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    /** 从脚本输出中提取 JSON 行 */
    private String extractJsonLine(String fullOutput) {
        if (fullOutput == null || fullOutput.isEmpty()) return null;
        String[] lines = fullOutput.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) return line;
        }
        return null;
    }

    private String resolveBaselineAgentType(Evaluation evaluation, Path baselineModelPath) {
        String fromBaselineId = extractAlgoFromBaselineId(evaluation.getBaselineId());
        if (fromBaselineId != null && !fromBaselineId.isBlank()) {
            return fromBaselineId.trim().toLowerCase(Locale.ROOT);
        }

        Path parent = baselineModelPath == null ? null : baselineModelPath.getParent();
        if (parent != null && parent.getFileName() != null) {
            String folder = parent.getFileName().toString();
            if (!folder.isBlank()) {
                return folder.trim().toLowerCase(Locale.ROOT);
            }
        }

        return "dqn";
    }

    private String extractAlgoFromBaselineId(String baselineId) {
        if (baselineId == null || baselineId.isBlank()) {
            return null;
        }
        int idx = baselineId.indexOf('-');
        if (idx < 0 || idx + 1 >= baselineId.length()) {
            return null;
        }
        return baselineId.substring(idx + 1);
    }
}
