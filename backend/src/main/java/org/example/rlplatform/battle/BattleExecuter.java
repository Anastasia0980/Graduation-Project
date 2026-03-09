package org.example.rlplatform.battle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.EvaluationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * 调用 Python battle_evaluator.py 并写入 evaluation_result（JSON + result_dir + winner）
 */
@Component
public class BattleExecuter {

    @Value("${evaluation.python:python}")
    private String pythonCmd;

    @Value("${evaluation.battle_script:scripts/battle_evaluator.py}")
    private String battleScriptPath;

    @Value("${evaluation.workspace:}")
    private String workspace;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EvaluationResultRepository evaluationResultRepository;

    public void executeBattle(Evaluation evaluation, String student1DirRel, String student2DirRel) {

        Path base = (workspace != null && !workspace.isBlank())
                ? Paths.get(workspace)
                : Paths.get(System.getProperty("user.dir"));

        // 脚本路径按“项目运行目录(user.dir)”解析（与你当前 EvaluationExecuter 一致的风格）
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path script = cwd.resolve(battleScriptPath).normalize();

        if (!script.toFile().exists()) {
            evaluation.setStatus("FAILED");
            evaluation.setErrorMessage("Battle script not found: " + script);
            saveEvaluationResult(evaluation, null, null);
            return;
        }

        // 结果目录固定为：results/<evaluationId>（相对 workspace）
        String resultDirRel = "results/" + evaluation.getId();

        ProcessBuilder pb = new ProcessBuilder(
                pythonCmd,
                script.toString(),
                "--workspace", base.toString(),
                "--student1_dir", student1DirRel,
                "--student2_dir", student2DirRel,
                "--env", evaluation.getEnvironment(),
                "--games", String.valueOf(evaluation.getEpisodes()),
                "--result_dir", resultDirRel
        );

        pb.redirectErrorStream(true);
        pb.directory(cwd.toFile());

        StringBuilder output = new StringBuilder();

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
            if (!finished) {
                process.destroyForcibly();
                evaluation.setStatus("FAILED");
                evaluation.setErrorMessage("Python battle timeout");
                saveEvaluationResult(evaluation, null, null);
                return;
            }

            int exitCode = process.exitValue();
            String out = output.toString().trim();

            String jsonLine = extractJsonLine(out);
            JsonNode root = null;
            if (jsonLine != null) {
                try { root = objectMapper.readTree(jsonLine); } catch (Exception ignored) {}
            }

            if (exitCode != 0) {
                evaluation.setStatus("FAILED");
                evaluation.setErrorMessage(root != null && root.has("error")
                        ? root.path("error").asText()
                        : (out.isEmpty() ? "Python battle failed (no output)" : out));
                saveEvaluationResult(evaluation, root, jsonLine);
                return;
            }

            if (root == null) {
                evaluation.setStatus("FAILED");
                evaluation.setErrorMessage("Invalid JSON from battle script: " +
                        (out.length() > 500 ? out.substring(0, 500) + "..." : out));
                saveEvaluationResult(evaluation, null, jsonLine);
                return;
            }

            String status = root.has("status") ? root.path("status").asText() : "FAILED";
            evaluation.setStatus("FINISHED".equals(status) ? "FINISHED" : "FAILED");
            if (root.has("error")) {
                evaluation.setErrorMessage(root.path("error").asText());
            }

            saveEvaluationResult(evaluation, root, jsonLine);

        } catch (Exception e) {
            evaluation.setStatus("FAILED");
            evaluation.setErrorMessage(e.getMessage());
            saveEvaluationResult(evaluation, null, null);
        }
    }

    private void saveEvaluationResult(Evaluation evaluation, JsonNode root, String jsonLine) {
        try {
            EvaluationResult er = new EvaluationResult();
            er.setEvaluationId(evaluation.getId());
            er.setResult("FINISHED".equals(evaluation.getStatus()) ? 1 : 0);

            if (jsonLine != null) er.setDetailedResults(jsonLine);
            else if (root != null) er.setDetailedResults(root.toString());

            if (root != null && root.has("result_dir")) {
                er.setResultDir(root.path("result_dir").asText());
            }

            if (root != null && root.has("winner")) {
                er.setWinner(root.path("winner").asInt());
            }

            evaluationResultRepository.save(er);
        } catch (Exception e) {
            evaluation.setStatus("FAILED");
            if (evaluation.getErrorMessage() == null || evaluation.getErrorMessage().isBlank()) {
                evaluation.setErrorMessage("Save battle result failed: " + e.getMessage());
            }
        }
    }

    private String extractJsonLine(String fullOutput) {
        if (fullOutput == null || fullOutput.isEmpty()) return null;
        String[] lines = fullOutput.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) return line;
        }
        return null;
    }
}