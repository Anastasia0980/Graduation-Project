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

@Service
public class BaselineServiceImpl implements BaselineService {

    @Autowired
    private BaselineRepository baselineRepository;

    // 例如：E:/.../saved_models
    @Value("${evaluation.baselineRoot:}")
    private String baselineRoot;

    private static final List<String> DIFFICULTIES = Arrays.asList("easy", "medium", "hard");

    @Override
    public Map<String, List<BaselineOption>> getBaselineCatalogByEnvironment(String environment) {
        Map<String, List<BaselineOption>> catalog = new HashMap<>();
        DIFFICULTIES.forEach(d -> catalog.put(d, new ArrayList<>()));

        if (baselineRoot == null || baselineRoot.isBlank() || environment == null || environment.isBlank()) {
            return catalog;
        }

        Path envBase = Paths.get(baselineRoot).resolve(environment);
        if (!Files.exists(envBase) || !Files.isDirectory(envBase)) {
            return catalog;
        }

        for (String difficulty : DIFFICULTIES) {
            Path diffDir = envBase.resolve(difficulty);
            if (!Files.exists(diffDir) || !Files.isDirectory(diffDir)) {
                continue;
            }

            try (DirectoryStream<Path> algoDirs = Files.newDirectoryStream(diffDir)) {
                for (Path algoDir : algoDirs) {
                    if (!Files.isDirectory(algoDir)) continue;

                    String algoDirName = algoDir.getFileName().toString();
                    String algoKey = String.valueOf(algoDirName).toLowerCase();
                    Path baselineFile = algoDir.resolve("baseline.pth");
                    if (!Files.exists(baselineFile)) continue;

                    String baselineId = difficulty + "-" + algoKey;

                    String modelPath = environment + "/" + difficulty + "/" + algoDirName + "/baseline.pth";

                    // upsert baseline record（但保留 isDeleted 语义：catalog 只返回未删除的资源）
                    Baseline record = baselineRepository
                            .findByEnvironmentAndDifficultyAndAlgorithm(environment, difficulty, algoKey)
                            .orElse(null);
                    if (record == null) {
                        Baseline newRecord = new Baseline();
                        newRecord.setEnvironment(environment);
                        newRecord.setDifficulty(difficulty);
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
                            .findByEnvironmentAndDifficultyAndAlgorithm(environment, difficulty, algoKey)
                            .orElse(null);
                    if (refreshed == null || Boolean.TRUE.equals(refreshed.getIsDeleted())) {
                        continue;
                    }

                    BaselineOption option = new BaselineOption();
                    option.setId(baselineId);
                    option.setLabel(algoDirName);
                    option.setAlgorithm(algoKey);
                    option.setModelPath(modelPath);
                    catalog.get(difficulty).add(option);
                }
            } catch (IOException ignored) {
                // catalog 兜底：出错时返回已收集的数据（更利于线上不因目录扫描中断）
            }

            catalog.get(difficulty).sort(Comparator.comparing(BaselineOption::getLabel, String.CASE_INSENSITIVE_ORDER));
        }

        return catalog;
    }

    private static String normalizeAlgorithmId(String algorithm) {
        if (algorithm == null) return "";
        // baseline 表/目录统一用小写算法名，避免大小写导致查找不到
        return algorithm.trim().toLowerCase();
    }

    @Override
    public BaselineOption uploadBaseline(String environment, String difficulty, String algorithm, MultipartFile model) {
        if (environment == null || environment.isBlank()) {
            throw new IllegalArgumentException("environment 不能为空");
        }
        if (difficulty == null || difficulty.isBlank()) {
            throw new IllegalArgumentException("difficulty 不能为空");
        }
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("algorithm 不能为空");
        }
        if (model == null || model.isEmpty()) {
            throw new IllegalArgumentException("baseline 文件不能为空");
        }

        String env = environment.trim();
        String diff = difficulty.trim().toLowerCase();
        String algo = normalizeAlgorithmId(algorithm);

        if (baselineRoot == null || baselineRoot.isBlank()) {
            throw new IllegalStateException("未配置 evaluation.baselineRoot");
        }

        Path algoDir = Paths.get(baselineRoot)
                .resolve(env)
                .resolve(diff)
                .resolve(algo);
        try {
            Files.createDirectories(algoDir);
        } catch (IOException e) {
            throw new RuntimeException("创建 baseline 目录失败", e);
        }

        Path baselineFile = algoDir.resolve("baseline.pth");
        try {
            // MultipartFile.transferTo 会覆盖同路径文件（不同实现有差异，这里尽量确保写入）
            model.transferTo(baselineFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException("baseline 文件写入失败", e);
        }

        String modelPath = env + "/" + diff + "/" + algo + "/baseline.pth";

        Baseline record = baselineRepository
                .findByEnvironmentAndDifficultyAndAlgorithm(env, diff, algo)
                .orElse(null);
        if (record == null) {
            record = new Baseline();
            record.setEnvironment(env);
            record.setDifficulty(diff);
            record.setAlgorithm(algo);
            record.setIsDeleted(false);
            record.setCreateTime(LocalDateTime.now());
        }
        record.setModelPath(modelPath);
        record.setIsDeleted(false);
        record.setUpdateTime(LocalDateTime.now());
        baselineRepository.save(record);

        BaselineOption option = new BaselineOption();
        option.setId(diff + "-" + algo.toLowerCase());
        option.setLabel(algo);
        option.setAlgorithm(algo);
        option.setModelPath(modelPath);
        return option;
    }

    @Override
    public void softDeleteBaseline(String environment, String difficulty, String algorithm) {
        if (environment == null || environment.isBlank()) {
            throw new IllegalArgumentException("environment 不能为空");
        }
        if (difficulty == null || difficulty.isBlank()) {
            throw new IllegalArgumentException("difficulty 不能为空");
        }
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("algorithm 不能为空");
        }

        String env = environment.trim();
        String diff = difficulty.trim().toLowerCase();
        String algo = normalizeAlgorithmId(algorithm);

        Baseline record = baselineRepository
                .findByEnvironmentAndDifficultyAndAlgorithm(env, diff, algo)
                .orElse(null);
        if (record == null) {
            throw new RuntimeException("baseline 不存在");
        }

        record.setIsDeleted(true);
        record.setUpdateTime(LocalDateTime.now());
        baselineRepository.save(record);
    }
}

