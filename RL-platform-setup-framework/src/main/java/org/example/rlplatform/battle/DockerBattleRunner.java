package org.example.rlplatform.battle;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.rlplatform.entity.BattleParticipant;
import org.example.rlplatform.entity.Evaluation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DockerBattleRunner {

    @Value("${sandbox.enabled:false}")
    private boolean enabled;

    @Value("${sandbox.docker-command:docker}")
    private String dockerCommand;

    @Value("${sandbox.docker-image:rl-battle-runner:1.0}")
    private String dockerImage;

    @Value("${sandbox.workspace:}")
    private String sandboxWorkspace;

    @Value("${sandbox.input-dir:}")
    private String sandboxInputDir;

    @Value("${sandbox.result-dir:}")
    private String sandboxResultDir;

    @Value("${sandbox.timeout-seconds:1800}")
    private long timeoutSeconds;

    @Value("${sandbox.cpu-limit:2}")
    private String cpuLimit;

    @Value("${sandbox.memory-limit:2g}")
    private String memoryLimit;

    @Value("${sandbox.pids-limit:256}")
    private String pidsLimit;

    @Value("${sandbox.seed:42}")
    private Integer seed;

    @Value("${sandbox.fps:10}")
    private Integer fps;

    public boolean isEnabled() {
        return enabled;
    }

    public DockerBattleResult run(Evaluation evaluation, BattleParticipant participant, String fallbackWorkspace) throws Exception {
        Path workspace = resolveWorkspace(fallbackWorkspace);
        Path inputRoot = requiredPath(sandboxInputDir, "sandbox.input-dir");
        Path resultRoot = requiredPath(sandboxResultDir, "sandbox.result-dir");

        Path evaluationInputDir = inputRoot.resolve(String.valueOf(evaluation.getId())).normalize();
        Path evaluationOutputDir = resultRoot.resolve(String.valueOf(evaluation.getId())).normalize();
        Files.createDirectories(evaluationInputDir);
        Files.createDirectories(evaluationOutputDir);

        copySubmission(resolveSubmissionDir(workspace, participant.getStudent1DirRel()),
                evaluationInputDir.resolve("student1"));
        copySubmission(resolveSubmissionDir(workspace, participant.getStudent2DirRel()),
                evaluationInputDir.resolve("student2"));

        Path stdoutLog = evaluationOutputDir.resolve("docker_stdout.log");
        Path stderrLog = evaluationOutputDir.resolve("docker_stderr.log");
        Path resultJson = evaluationOutputDir.resolve("result.json");
        Path resultBase = evaluationOutputDir.resolve("video_0");
        String containerName = buildContainerName(evaluation.getId());

        List<String> command = buildDockerCommand(evaluation, evaluationInputDir, evaluationOutputDir, containerName);
        log.info("Battle sandbox id={} command={}", evaluation.getId(), String.join(" ", command));

        Instant start = Instant.now();
        Process process = new ProcessBuilder(command)
                .redirectOutput(stdoutLog.toFile())
                .redirectError(stderrLog.toFile())
                .start();

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            stopContainer(containerName);
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            return new DockerBattleResult(-1, true, elapsedMs, evaluationOutputDir, resultJson,
                    resultBase, readIfExists(stdoutLog), readIfExists(stderrLog));
        }

        int exitCode = process.exitValue();
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        return new DockerBattleResult(exitCode, false, elapsedMs, evaluationOutputDir, resultJson,
                resultBase, readIfExists(stdoutLog), readIfExists(stderrLog));
    }

    private List<String> buildDockerCommand(Evaluation evaluation, Path inputDir, Path outputDir, String containerName) {
        List<String> command = new ArrayList<>();
        command.add(dockerCommand);
        command.add("run");
        command.add("--rm");
        command.add("--name");
        command.add(containerName);
        command.add("--network");
        command.add("none");
        if (hasText(cpuLimit)) {
            command.add("--cpus");
            command.add(cpuLimit);
        }
        if (hasText(memoryLimit)) {
            command.add("--memory");
            command.add(memoryLimit);
        }
        if (hasText(pidsLimit)) {
            command.add("--pids-limit");
            command.add(pidsLimit);
        }
        command.add("-v");
        command.add(inputDir.toAbsolutePath() + ":/input:ro");
        command.add("-v");
        command.add(outputDir.toAbsolutePath() + ":/output");
        command.add(dockerImage);
        command.add("--input");
        command.add("/input");
        command.add("--output");
        command.add("/output");
        command.add("--student1-dir");
        command.add("/input/student1");
        command.add("--student2-dir");
        command.add("/input/student2");
        command.add("--env");
        command.add(evaluation.getEnvironment());
        command.add("--games");
        command.add(String.valueOf(evaluation.getEpisodes()));
        command.add("--seed");
        command.add(String.valueOf(seed));
        command.add("--fps");
        command.add(String.valueOf(fps));
        return command;
    }

    private Path resolveWorkspace(String fallbackWorkspace) {
        String base = hasText(sandboxWorkspace) ? sandboxWorkspace : fallbackWorkspace;
        if (!hasText(base)) {
            base = Paths.get(System.getProperty("user.dir")).toString();
        }
        return Paths.get(base).toAbsolutePath().normalize();
    }

    private Path requiredPath(String value, String propertyName) {
        if (!hasText(value)) {
            throw new IllegalStateException(propertyName + " is required when sandbox.enabled=true");
        }
        return Paths.get(value).toAbsolutePath().normalize();
    }

    private Path resolveSubmissionDir(Path workspace, String dir) {
        if (!hasText(dir)) {
            throw new IllegalArgumentException("battle participant submission directory is blank");
        }
        Path path = Paths.get(dir);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return workspace.resolve(dir).normalize();
    }

    private void copySubmission(Path sourceDir, Path targetDir) throws Exception {
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("submission directory not found: " + sourceDir);
        }
        Path config = sourceDir.resolve("config.json");
        if (!Files.isRegularFile(config)) {
            throw new IllegalArgumentException("config.json not found in " + sourceDir);
        }
        Path model = findModel(sourceDir);
        if (model == null) {
            throw new IllegalArgumentException("model.pt/model.pth not found in " + sourceDir);
        }

        Files.createDirectories(targetDir);
        Files.deleteIfExists(targetDir.resolve("model.pt"));
        Files.deleteIfExists(targetDir.resolve("model.pth"));
        Files.copy(config, targetDir.resolve("config.json"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(model, targetDir.resolve(model.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
    }

    private Path findModel(Path sourceDir) {
        Path pt = sourceDir.resolve("model.pt");
        if (Files.isRegularFile(pt)) {
            return pt;
        }
        Path pth = sourceDir.resolve("model.pth");
        if (Files.isRegularFile(pth)) {
            return pth;
        }
        return null;
    }

    private void stopContainer(String containerName) {
        try {
            new ProcessBuilder(dockerCommand, "rm", "-f", containerName)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to stop timed out battle container name={}", containerName, e);
        }
    }

    private String buildContainerName(Long evaluationId) {
        return ("rl-battle-" + evaluationId + "-" + System.currentTimeMillis())
                .replaceAll("[^a-zA-Z0-9_.-]", "-");
    }

    private String readIfExists(Path path) {
        try {
            return Files.exists(path) ? Files.readString(path) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Getter
    public static class DockerBattleResult {
        private final int exitCode;
        private final boolean timedOut;
        private final long elapsedMs;
        private final Path outputDir;
        private final Path resultJson;
        private final Path hostResultBase;
        private final String stdout;
        private final String stderr;

        public DockerBattleResult(int exitCode, boolean timedOut, long elapsedMs, Path outputDir,
                                  Path resultJson, Path hostResultBase, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.timedOut = timedOut;
            this.elapsedMs = elapsedMs;
            this.outputDir = outputDir;
            this.resultJson = resultJson;
            this.hostResultBase = hostResultBase;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
