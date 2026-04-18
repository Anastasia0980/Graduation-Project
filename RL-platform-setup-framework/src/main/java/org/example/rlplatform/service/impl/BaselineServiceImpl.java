package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.BaselineRepository;
import org.example.rlplatform.entity.Baseline;
import org.example.rlplatform.entity.BaselineOption;
import org.example.rlplatform.service.BaselineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BaselineServiceImpl implements BaselineService {

    @Autowired
    private BaselineRepository baselineRepository;

    @Value("${evaluation.baselineRoot:}")
    private String baselineRoot;

    private static final Pattern TASK_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    public Map<String, List<BaselineOption>> getBaselineCatalogByEnvironment(String environment) {
        Map<String, List<BaselineOption>> catalog = new LinkedHashMap<>();
        if (baselineRoot == null || baselineRoot.isBlank() || environment == null || environment.isBlank()) {
            return catalog;
        }

        Path envBase = Paths.get(baselineRoot).resolve(environment);
        if (!Files.exists(envBase) || !Files.isDirectory(envBase)) {
            return catalog;
        }

        List<String> taskIds;
        try (Stream<Path> stream = Files.list(envBase)) {
            taskIds = stream
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> !name.startsWith("."))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return catalog;
        }

        for (String taskId : taskIds) {
            catalog.putIfAbsent(taskId, new ArrayList<>());
            Path taskDir = envBase.resolve(taskId);
            try (DirectoryStream<Path> algoDirs = Files.newDirectoryStream(taskDir)) {
                for (Path algoDir : algoDirs) {
                    if (!Files.isDirectory(algoDir)) continue;

                    String algoDirName = algoDir.getFileName().toString();
                    String algoKey = String.valueOf(algoDirName).toLowerCase();
                    Path baselineFile = algoDir.resolve("baseline.pth");
                    if (!Files.exists(baselineFile)) continue;

                    String baselineId = taskId + "-" + algoKey;

                    String modelPath = environment + "/" + taskId + "/" + algoDirName + "/baseline.pth";

                    Baseline record = baselineRepository
                            .findByEnvironmentAndTaskIdAndAlgorithm(environment, taskId, algoKey)
                            .orElse(null);
                    if (record == null) {
                        Baseline newRecord = new Baseline();
                        newRecord.setEnvironment(environment);
                        newRecord.setTaskId(taskId);
                        newRecord.setAlgorithm(algoKey);
                        newRecord.setModelPath(modelPath);
                        newRecord.setIsDeleted(false);
                        newRecord.setCreateTime(LocalDateTime.now());
                        newRecord.setUpdateTime(LocalDateTime.now());
                        baselineRepository.save(newRecord);
                    } else {
                        record.setModelPath(modelPath);
                        record.setUpdateTime(LocalDateTime.now());
                        baselineRepository.save(record);
                    }

                    Baseline refreshed = baselineRepository
                            .findByEnvironmentAndTaskIdAndAlgorithm(environment, taskId, algoKey)
                            .orElse(null);
                    if (refreshed == null || Boolean.TRUE.equals(refreshed.getIsDeleted())) {
                        continue;
                    }

                    BaselineOption option = new BaselineOption();
                    option.setId(baselineId);
                    option.setLabel(algoDirName);
                    option.setAlgorithm(algoKey);
                    option.setModelPath(modelPath);
                    catalog.get(taskId).add(option);
                }
            } catch (IOException ignored) {
                // catalog 兜底
            }

            catalog.get(taskId).sort(Comparator.comparing(BaselineOption::getLabel, String.CASE_INSENSITIVE_ORDER));
        }

        return catalog;
    }

    private static String normalizeAlgorithmId(String algorithm) {
        if (algorithm == null) return "";
        return algorithm.trim().toLowerCase();
    }

    @Override
    public BaselineOption uploadBaseline(String environment, String taskId, String algorithm, MultipartFile model) {
        if (environment == null || environment.isBlank()) {
            throw new IllegalArgumentException("environment 不能为空");
        }
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId 不能为空");
        }
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("algorithm 不能为空");
        }
        if (model == null || model.isEmpty()) {
            throw new IllegalArgumentException("baseline 文件不能为空");
        }

        String env = environment.trim();
        String task = taskId.trim();
        if (!TASK_KEY_PATTERN.matcher(task).matches()) {
            throw new IllegalArgumentException("taskId 非法：1-64 位字母数字下划线或连字符");
        }
        String algo = normalizeAlgorithmId(algorithm);

        if (baselineRoot == null || baselineRoot.isBlank()) {
            throw new IllegalStateException("未配置 evaluation.baselineRoot");
        }

        Path algoDir = Paths.get(baselineRoot)
                .resolve(env)
                .resolve(task)
                .resolve(algo);
        try {
            Files.createDirectories(algoDir);
        } catch (IOException e) {
            throw new RuntimeException("创建 baseline 目录失败", e);
        }

        Path baselineFile = algoDir.resolve("baseline.pth");
        try {
            model.transferTo(baselineFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException("baseline 文件写入失败", e);
        }

        String modelPath = env + "/" + task + "/" + algo + "/baseline.pth";

        Baseline record = baselineRepository
                .findByEnvironmentAndTaskIdAndAlgorithm(env, task, algo)
                .orElse(null);
        if (record == null) {
            record = new Baseline();
            record.setEnvironment(env);
            record.setTaskId(task);
            record.setAlgorithm(algo);
            record.setIsDeleted(false);
            record.setCreateTime(LocalDateTime.now());
        }
        record.setModelPath(modelPath);
        record.setIsDeleted(false);
        record.setUpdateTime(LocalDateTime.now());
        baselineRepository.save(record);

        BaselineOption option = new BaselineOption();
        option.setId(task + "-" + algo.toLowerCase(Locale.ROOT));
        option.setLabel(algo);
        option.setAlgorithm(algo);
        option.setModelPath(modelPath);
        return option;
    }

    @Override
    public void softDeleteBaseline(String environment, String taskId, String algorithm) {
        if (environment == null || environment.isBlank()) {
            throw new IllegalArgumentException("environment 不能为空");
        }
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId 不能为空");
        }
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("algorithm 不能为空");
        }

        String env = environment.trim();
        String task = taskId.trim();
        if (!TASK_KEY_PATTERN.matcher(task).matches()) {
            throw new IllegalArgumentException("taskId 非法：1-64 位字母数字下划线或连字符");
        }
        String algo = normalizeAlgorithmId(algorithm);

        Baseline record = baselineRepository
                .findByEnvironmentAndTaskIdAndAlgorithm(env, task, algo)
                .orElse(null);
        if (record == null) {
            throw new RuntimeException("baseline 不存在");
        }

        record.setIsDeleted(true);
        record.setUpdateTime(LocalDateTime.now());
        baselineRepository.save(record);
    }
}
