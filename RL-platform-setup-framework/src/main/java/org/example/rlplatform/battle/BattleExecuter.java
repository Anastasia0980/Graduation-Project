package org.example.rlplatform.battle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.entity.BattleEnvironment;
import org.example.rlplatform.entity.BattleParticipant;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.EvaluationResult;
import org.example.rlplatform.entity.EvaluationStatus;
import org.example.rlplatform.service.BattleEnvironmentService;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class BattleExecuter {

    @Value("${battle.python:python}")
    private String pythonCmd;

    @Value("${battle.script:scripts/battle_evaluator.py}")
    private String battleScriptPath;

    @Value("${evaluation.workspace:}")
    private String workspaceConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EvaluationResultRepository evaluationResultRepository;

    @Autowired
    private BattleEnvironmentService battleEnvironmentService;

    public void executeBattle(Evaluation evaluation, BattleParticipant participant) {
        final Long evalId = evaluation.getId();
        BattleEnvironment battleEnvironment = battleEnvironmentService.getReadyByCode(evaluation.getEnvironment());
        log.info("Battle evaluation start id={} env={} games={} student1Dir={} student2Dir={}",
                evalId, evaluation.getEnvironment(), evaluation.getEpisodes(),
                participant.getStudent1DirRel(), participant.getStudent2DirRel());

        String finalPythonCmd = resolvePythonCmd(battleEnvironment);
        String base = resolveWorkspace(battleEnvironment);
        Path script = resolveScriptPath(battleEnvironment);

        if (!script.toFile().exists()) {
            log.warn("Battle evaluation id={} aborted: script not found path={}", evalId, script);
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorMessage("Battle script not found: " + script);
            saveEvaluationResult(evaluation, null, null, base);
            return;
        }

        String resultBase = Paths.get("results", String.valueOf(evaluation.getId()), "video_0")
                .toString()
                .replace("\\", "/");

        ProcessBuilder pb = new ProcessBuilder(
                finalPythonCmd, script.toString(),
                "--workspace", base,
                "--student1_dir", participant.getStudent1DirRel(),
                "--student2_dir", participant.getStudent2DirRel(),
                "--env", evaluation.getEnvironment(),
                "--games", String.valueOf(evaluation.getEpisodes()),
                "--result_base", resultBase
        );

        if (battleEnvironment != null
                && Boolean.TRUE.equals(battleEnvironment.getIsGpu())
                && battleEnvironment.getCudaDevice() != null
                && !battleEnvironment.getCudaDevice().isBlank()) {
            pb.environment().put("CUDA_VISIBLE_DEVICES", battleEnvironment.getCudaDevice());
        }

        pb.redirectErrorStream(true);
        Path cwd = script.getParent();
        if (cwd != null) {
            pb.directory(cwd.toFile());
        }

        log.info("Battle evaluation id={} spawning process python={} script={} cwd={} resultBase={}",
                evalId, finalPythonCmd, script, cwd, resultBase);

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
                log.warn("Battle evaluation id={} failed: python timeout after {}ms (limit 30m)", evalId, elapsedMs);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("Battle python execution timeout");
                saveEvaluationResult(evaluation, null, null, base);
                return;
            }

            int exitCode = process.exitValue();
            String out = output.toString().trim();
            if (log.isDebugEnabled()) {
                log.debug("Battle evaluation id={} raw output ({} chars): {}", evalId, out.length(),
                        out.length() > 4000 ? out.substring(0, 4000) + "..." : out);
            }

            String jsonLine = extractJsonLine(out);
            JsonNode root = null;
            if (jsonLine != null) {
                try {
                    root = objectMapper.readTree(jsonLine);
                } catch (Exception ignored) {
                }
            }

            if (exitCode != 0) {
                String errMsg = root != null && root.has("error")
                        ? root.path("error").asText()
                        : (out.isEmpty() ? "Battle script failed (no output)" : out);
                log.warn("Battle evaluation id={} python exitCode={} after {}ms error={}", evalId, exitCode, elapsedMs, errMsg);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage(errMsg);
                log.info("Battle evaluation id={} finished status={} in {}ms", evalId, evaluation.getStatus(), elapsedMs);
                saveEvaluationResult(evaluation, root, jsonLine, base);
                return;
            }

            if (root == null) {
                String preview = out.length() > 500 ? out.substring(0, 500) + "..." : out;
                log.warn("Battle evaluation id={} failed: no valid JSON line in output after {}ms preview={}",
                        evalId, elapsedMs, preview);
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("Invalid JSON from battle script. Output: " +
                        (out.length() > 500 ? out.substring(0, 500) + "..." : out));
                saveEvaluationResult(evaluation, null, jsonLine, base);
                return;
            }

            String status = root.has("status") ? root.path("status").asText() : EvaluationStatus.FINISHED.name();
            try {
                evaluation.setStatus(EvaluationStatus.valueOf(status.toUpperCase()));
            } catch (Exception e) {
                evaluation.setStatus(EvaluationStatus.FINISHED);
            }

            if (root.has("error") && !root.path("error").isNull() && !root.path("error").asText().isBlank()) {
                evaluation.setErrorMessage(root.path("error").asText());
            }

            saveEvaluationResult(evaluation, root, jsonLine, base);

        } catch (Exception e) {
            log.error("Battle evaluation id={} unexpected error", evalId, e);
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorMessage(e.getMessage());
            saveEvaluationResult(evaluation, null, null, base);
        }
    }

    private String resolvePythonCmd(BattleEnvironment battleEnvironment) {
        if (battleEnvironment != null
                && battleEnvironment.getPythonPath() != null
                && !battleEnvironment.getPythonPath().isBlank()) {
            return battleEnvironment.getPythonPath();
        }
        return (pythonCmd == null || pythonCmd.isBlank()) ? "python" : pythonCmd;
    }

    private String resolveWorkspace(BattleEnvironment battleEnvironment) {
        if (battleEnvironment != null
                && battleEnvironment.getWorkspacePath() != null
                && !battleEnvironment.getWorkspacePath().isBlank()) {
            return battleEnvironment.getWorkspacePath();
        }
        return (workspaceConfig != null && !workspaceConfig.isBlank())
                ? workspaceConfig
                : Paths.get(System.getProperty("user.dir")).toString();
    }

    private Path resolveScriptPath(BattleEnvironment battleEnvironment) {
        if (battleEnvironment != null
                && battleEnvironment.getScriptPath() != null
                && !battleEnvironment.getScriptPath().isBlank()) {
            return Paths.get(battleEnvironment.getScriptPath()).normalize();
        }
        Path cwd = Paths.get(System.getProperty("user.dir"));
        return cwd.resolve(battleScriptPath).normalize();
    }

    private void saveEvaluationResult(Evaluation evaluation, JsonNode root, String jsonLine, String workspaceBase) {
        try {
            EvaluationResult er = new EvaluationResult();
            er.setEvaluationId(evaluation.getId());
            er.setResult(evaluation.getStatus() == EvaluationStatus.FINISHED ? 0 : 1);

            String detailedResults = resolveDetailedResults(root, jsonLine, workspaceBase);
            if (detailedResults != null && !detailedResults.isBlank()) {
                er.setDetailedResults(detailedResults);
            } else if (jsonLine != null) {
                er.setDetailedResults(jsonLine);
            } else if (root != null) {
                er.setDetailedResults(root.toString());
            }

            if (root != null && root.has("winner")) {
                er.setWinner(root.path("winner").asInt());
            }

            if (root != null && root.has("result_dir")) {
                er.setResultDir(root.path("result_dir").asText());
            }

            evaluationResultRepository.save(er);
            log.debug("Battle evaluation id={} result row saved", evaluation.getId());
        } catch (Exception e) {
            log.error("Battle evaluation id={} save EvaluationResult failed", evaluation.getId(), e);
            evaluation.setStatus(EvaluationStatus.FAILED);
            if (evaluation.getErrorMessage() == null || evaluation.getErrorMessage().isBlank()) {
                evaluation.setErrorMessage("Save battle result failed: " + e.getMessage());
            }
        }
    }

    private String resolveDetailedResults(JsonNode root, String jsonLine, String workspaceBase) {
        if (root == null || !root.has("result_dir") || workspaceBase == null || workspaceBase.isBlank()) {
            return jsonLine;
        }

        try {
            String resultDir = root.path("result_dir").asText();
            if (resultDir == null || resultDir.isBlank()) {
                return jsonLine;
            }

            Path resultJsonPath = Paths.get(workspaceBase, resultDir + "_result.json").normalize();
            if (Files.exists(resultJsonPath) && Files.isRegularFile(resultJsonPath)) {
                return Files.readString(resultJsonPath, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }

        return jsonLine;
    }

    private String extractJsonLine(String fullOutput) {
        if (fullOutput == null || fullOutput.isEmpty()) {
            return null;
        }
        String[] lines = fullOutput.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                return line;
            }
        }
        return null;
    }
}