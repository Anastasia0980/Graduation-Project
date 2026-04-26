package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.BattleEnvironmentRepository;
import org.example.rlplatform.Repository.TeacherUploadResourceRepository;
import org.example.rlplatform.entity.BattleEnvironment;
import org.example.rlplatform.entity.TeacherUploadResource;
import org.example.rlplatform.service.BattleEnvironmentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BattleEnvironmentServiceImpl implements BattleEnvironmentService {

    private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BattleEnvironmentRepository battleEnvironmentRepository;
    private final TeacherUploadResourceRepository teacherUploadResourceRepository;
    private final Executor evaluationExecutor;

    @Value("${conda.executable:conda}")
    private String condaExecutable;

    @Value("${conda.envs-root:}")
    private String condaEnvsRoot;

    @Value("${battle-env.root:}")
    private String battleEnvRoot;

    @Value("${battle-env.python-version:3.9}")
    private String battleEnvPythonVersion;

    @Value("${evaluation.workspace:}")
    private String defaultWorkspace;

    @Value("${platform.upload.base-dir:uploads}")
    private String uploadBaseDir;

    public BattleEnvironmentServiceImpl(BattleEnvironmentRepository battleEnvironmentRepository,
                                        TeacherUploadResourceRepository teacherUploadResourceRepository,
                                        @Qualifier("evaluationExecutor") Executor evaluationExecutor) {
        this.battleEnvironmentRepository = battleEnvironmentRepository;
        this.teacherUploadResourceRepository = teacherUploadResourceRepository;
        this.evaluationExecutor = evaluationExecutor;
    }

    @Override
    public BattleEnvironment create(String name,
                                    String code,
                                    Boolean isGpu,
                                    String cudaDevice,
                                    MultipartFile requirements,
                                    MultipartFile script) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("环境名称不能为空");
        }
        if (code == null || code.isBlank()) {
            throw new RuntimeException("环境编码不能为空");
        }
        String normalizedCode = normalizeCode(code);
        if (battleEnvironmentRepository.existsByCodeAndIsDeletedFalse(normalizedCode)) {
            throw new RuntimeException("环境编码已存在");
        }
        if (requirements == null || requirements.isEmpty()) {
            throw new RuntimeException("请上传 requirements.txt");
        }
        if (script == null || script.isEmpty()) {
            throw new RuntimeException("请上传对战脚本");
        }
        String scriptName = Objects.requireNonNullElse(script.getOriginalFilename(), "battle.py").toLowerCase(Locale.ROOT);
        if (!scriptName.endsWith(".py")) {
            throw new RuntimeException("对战脚本必须是 .py 文件");
        }

        try {
            Path envRoot = resolveEnvironmentRoot(normalizedCode);
            Files.createDirectories(envRoot);
            Path requirementsPath = envRoot.resolve("requirements.txt");
            Path scriptPath = envRoot.resolve("battle_runner.py");
            requirements.transferTo(requirementsPath);
            script.transferTo(scriptPath);

            BattleEnvironment env = new BattleEnvironment();
            env.setName(name.trim());
            env.setCode(normalizedCode);
            env.setModeType("BATTLE");
            env.setCondaEnvName(buildCondaEnvName(normalizedCode));
            env.setRequirementsPath(requirementsPath.toString());
            env.setScriptPath(scriptPath.toString());
            env.setWorkspacePath(resolveWorkspaceRoot(envRoot).toString());
            env.setStatus("CREATING");
            env.setInstallLog("已接收上传文件，准备创建 Conda 环境...\n");
            env.setIsGpu(Boolean.TRUE.equals(isGpu));
            env.setCudaDevice((cudaDevice == null || cudaDevice.isBlank()) ? null : cudaDevice.trim());
            env.setIsDeleted(false);
            env.setCreateTime(LocalDateTime.now());
            env.setUpdateTime(LocalDateTime.now());
            battleEnvironmentRepository.save(env);

            evaluationExecutor.execute(() -> createCondaEnvironment(env.getId()));
            return env;
        } catch (Exception e) {
            throw new RuntimeException("环境创建初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BattleEnvironment> listAll() {
        return battleEnvironmentRepository.findAllByOrderByIdDesc();
    }

    @Override
    public List<BattleEnvironment> listReadyBattleEnvironments() {
        return battleEnvironmentRepository.findByStatusAndIsDeletedFalseOrderByIdDesc("READY");
    }

    @Override
    public BattleEnvironment getReadyByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return battleEnvironmentRepository.findByCodeAndModeTypeAndIsDeletedFalse(code.trim(), "BATTLE")
                .filter(env -> "READY".equalsIgnoreCase(env.getStatus()))
                .orElse(null);
    }

    @Override
    public String resolveWorkspace(String code) {
        BattleEnvironment env = getReadyByCode(code);
        if (env != null && env.getWorkspacePath() != null && !env.getWorkspacePath().isBlank()) {
            return env.getWorkspacePath();
        }
        return defaultWorkspace;
    }

    @Override
    public void disable(Integer id) {
        BattleEnvironment env = battleEnvironmentRepository.findByIdAndIsDeletedFalse(id);
        if (env == null) {
            throw new RuntimeException("环境不存在");
        }
        if ("DELETED".equalsIgnoreCase(env.getStatus())) {
            throw new RuntimeException("环境已删除，不能重复停用");
        }
        env.setStatus("DISABLED");
        env.setUpdateTime(LocalDateTime.now());
        appendLog(env, "环境已停用");
        battleEnvironmentRepository.save(env);
    }

    @Override
    public void delete(Integer id) {
        BattleEnvironment env = battleEnvironmentRepository.findByIdAndIsDeletedFalse(id);
        if (env == null) {
            throw new RuntimeException("环境不存在或已删除");
        }
        if ("CREATING".equalsIgnoreCase(env.getStatus())) {
            throw new RuntimeException("环境正在创建中，暂不支持删除");
        }

        String originalCode = env.getCode();
        String deletedCode = buildDeletedCode(originalCode, env.getId());

        StringBuilder log = new StringBuilder();
        if (env.getInstallLog() != null && !env.getInstallLog().isBlank()) {
            log.append(env.getInstallLog());
            if (!env.getInstallLog().endsWith("\n")) {
                log.append('\n');
            }
        }
        log.append("开始删除环境资源...\n");

        try {
            removeCondaEnvironment(env, log);
        } catch (Exception e) {
            log.append("删除 Conda 环境失败: ").append(e.getMessage()).append('\n');
        }

        try {
            deleteUploadedFiles(env, log);
        } catch (Exception e) {
            log.append("删除上传文件失败: ").append(e.getMessage()).append('\n');
        }

        log.append("原环境编码: ").append(originalCode).append('\n');
        log.append("删除后历史编码: ").append(deletedCode).append('\n');

        env.setCode(deletedCode);
        env.setStatus("DELETED");
        env.setPythonPath(null);
        env.setIsDeleted(true);
        env.setInstallLog(log.toString());
        env.setUpdateTime(LocalDateTime.now());
        battleEnvironmentRepository.save(env);
    }

    @Override
    public List<TeacherUploadResource> listTeacherResources() {
        return teacherUploadResourceRepository.findAllByOrderByIdDesc();
    }

    @Override
    public List<TeacherUploadResource> uploadTeacherResources(String name, MultipartFile[] files) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("请输入文件名称");
        }
        if (files == null || files.length == 0) {
            throw new RuntimeException("请至少选择一个文件");
        }

        List<TeacherUploadResource> savedList = new ArrayList<>();
        Path rootDir = resolveTeacherResourceRoot();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new RuntimeException("创建上传目录失败", e);
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                TeacherUploadResource failed = buildFailedResource(name, file, "空文件");
                savedList.add(teacherUploadResourceRepository.save(failed));
                continue;
            }

            String originalFilename = sanitizeFilename(file.getOriginalFilename());
            String storedFilename = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
            Path targetPath = rootDir.resolve(storedFilename);

            try {
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                TeacherUploadResource resource = new TeacherUploadResource();
                resource.setName(name.trim());
                resource.setOriginalFilename(originalFilename);
                resource.setStoredFilename(storedFilename);
                resource.setFilePath(targetPath.toString());
                resource.setContentType(file.getContentType());
                resource.setFileSize(file.getSize());
                resource.setStatus("SUCCESS");
                resource.setIsDeleted(false);
                resource.setCreateTime(LocalDateTime.now());
                resource.setUpdateTime(LocalDateTime.now());
                savedList.add(teacherUploadResourceRepository.save(resource));
            } catch (Exception e) {
                TeacherUploadResource failed = buildFailedResource(name, file, e.getMessage());
                savedList.add(teacherUploadResourceRepository.save(failed));
            }
        }
        return savedList;
    }

    @Override
    public void deleteTeacherResource(Integer id) {
        TeacherUploadResource resource = teacherUploadResourceRepository.findByIdAndIsDeletedFalse(id);
        if (resource == null) {
            throw new RuntimeException("文件不存在或已删除");
        }
        Path filePath = resource.getFilePath() == null ? null : Paths.get(resource.getFilePath());
        try {
            if (filePath != null && Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (Exception ignored) {
        }
        resource.setStatus("DELETED");
        resource.setIsDeleted(true);
        resource.setUpdateTime(LocalDateTime.now());
        teacherUploadResourceRepository.save(resource);
    }

    @Override
    public Map<String, Object> downloadEnvironmentPackage(Integer id) {
        BattleEnvironment env = battleEnvironmentRepository.findByIdAndIsDeletedFalse(id);
        if (env == null || !"READY".equalsIgnoreCase(env.getStatus())) {
            throw new RuntimeException("环境不存在或当前不可下载");
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            addZipFile(zos, env.getRequirementsPath(), "requirements.txt");
            addZipFile(zos, env.getScriptPath(), "battle_runner.py");
            zos.finish();
            Map<String, Object> data = new HashMap<>();
            data.put("bytes", baos.toByteArray());
            data.put("fileName", safeZipName(env.getName() + ".zip"));
            return data;
        } catch (Exception e) {
            throw new RuntimeException("环境压缩包生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> downloadTeacherResource(Integer id) {
        TeacherUploadResource resource = teacherUploadResourceRepository.findByIdAndIsDeletedFalse(id);
        if (resource == null || !"SUCCESS".equalsIgnoreCase(resource.getStatus())) {
            throw new RuntimeException("文件不存在或当前不可下载");
        }
        Path filePath = Paths.get(resource.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("文件不存在，无法下载");
        }
        Resource fileResource = new FileSystemResource(filePath);
        Map<String, Object> data = new HashMap<>();
        data.put("resource", fileResource);
        data.put("fileName", resource.getOriginalFilename());
        data.put("fileSize", resource.getFileSize() == null ? 0L : resource.getFileSize());
        data.put("contentType", resource.getContentType() == null || resource.getContentType().isBlank()
                ? "application/octet-stream" : resource.getContentType());
        return data;
    }

    @Override
    public List<Map<String, Object>> listStudentDownloadItems() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (BattleEnvironment env : listReadyBattleEnvironments()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", env.getId());
            item.put("itemType", "ENVIRONMENT");
            item.put("name", env.getName());
            item.put("subtitle", env.getCode());
            item.put("status", env.getStatus());
            item.put("createTime", env.getCreateTime());
            item.put("downloadUrl", "/battle-environments/" + env.getId() + "/download");
            item.put("downloadName", env.getName() + ".zip");
            result.add(item);
        }

        for (TeacherUploadResource resource : teacherUploadResourceRepository.findByStatusAndIsDeletedFalseOrderByIdDesc("SUCCESS")) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", resource.getId());
            item.put("itemType", "FILE");
            item.put("name", resource.getName());
            item.put("subtitle", resource.getOriginalFilename());
            item.put("status", resource.getStatus());
            item.put("createTime", resource.getCreateTime());
            item.put("downloadUrl", "/battle-environments/resources/" + resource.getId() + "/download");
            item.put("downloadName", resource.getOriginalFilename());
            result.add(item);
        }

        result.sort((a, b) -> {
            LocalDateTime t1 = (LocalDateTime) a.get("createTime");
            LocalDateTime t2 = (LocalDateTime) b.get("createTime");
            if (t1 == null && t2 == null) {
                return 0;
            }
            if (t1 == null) {
                return 1;
            }
            if (t2 == null) {
                return -1;
            }
            return t2.compareTo(t1);
        });
        return result;
    }

    private void createCondaEnvironment(Integer environmentId) {
        BattleEnvironment env = battleEnvironmentRepository.findByIdAndIsDeletedFalse(environmentId);
        if (env == null) {
            return;
        }
        StringBuilder log = new StringBuilder();
        try {
            Path workspaceRoot = Paths.get(env.getWorkspacePath());
            Files.createDirectories(workspaceRoot);
            log.append("开始创建 Conda 环境: ").append(env.getCondaEnvName()).append('\n');
            runCommand(log, buildCondaCommand("create", "-y", "-n", env.getCondaEnvName(), "python=" + battleEnvPythonVersion), null);
            log.append("开始安装 requirements.txt\n");
            runCommand(log, buildCondaCommand("run", "-n", env.getCondaEnvName(), "python", "-m", "pip", "install", "-r", env.getRequirementsPath()), null);

            Path pythonPath = resolveCondaPythonPath(env.getCondaEnvName());
            env.setPythonPath(pythonPath.toString());
            env.setStatus("READY");
            log.append("环境创建完成\n");
        } catch (Exception e) {
            env.setStatus("FAILED");
            log.append("环境创建失败: ").append(e.getMessage()).append('\n');
        }
        env.setInstallLog(log.toString());
        env.setUpdateTime(LocalDateTime.now());
        battleEnvironmentRepository.save(env);
    }

    private void removeCondaEnvironment(BattleEnvironment env, StringBuilder log) {
        if (env.getCondaEnvName() == null || env.getCondaEnvName().isBlank()) {
            log.append("未检测到 Conda 环境名，跳过删除 Conda 环境\n");
            return;
        }
        log.append("开始删除 Conda 环境: ").append(env.getCondaEnvName()).append('\n');
        runCommand(log, buildCondaCommand("remove", "-y", "-n", env.getCondaEnvName(), "--all"), null);
        log.append("Conda 环境删除完成\n");
    }

    private void deleteUploadedFiles(BattleEnvironment env, StringBuilder log) throws IOException {
        Path envRoot = resolveEnvironmentRoot(env.getCode());

        if (env.getRequirementsPath() != null && !env.getRequirementsPath().isBlank()) {
            deleteFileIfExists(Paths.get(env.getRequirementsPath()), log, "requirements.txt");
        }
        if (env.getScriptPath() != null && !env.getScriptPath().isBlank()) {
            deleteFileIfExists(Paths.get(env.getScriptPath()), log, "对战脚本");
        }

        if (env.getWorkspacePath() != null && !env.getWorkspacePath().isBlank()) {
            Path workspacePath = Paths.get(env.getWorkspacePath());
            if (workspacePath.startsWith(envRoot) && Files.exists(workspacePath)) {
                deleteDirectoryIfExists(workspacePath, log, "环境工作目录");
            } else if (Files.exists(workspacePath)) {
                log.append("工作目录不是该环境独占目录，已跳过删除: ").append(workspacePath).append('\n');
            }
        }

        if (Files.exists(envRoot)) {
            deleteDirectoryIfExists(envRoot, log, "环境根目录");
        }
    }

    private void deleteFileIfExists(Path filePath, StringBuilder log, String label) throws IOException {
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.append(label).append("已删除: ").append(filePath).append('\n');
        } else {
            log.append(label).append("不存在，跳过: ").append(filePath).append('\n');
        }
    }

    private void deleteDirectoryIfExists(Path dirPath, StringBuilder log, String label) throws IOException {
        Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        log.append(label).append("已删除: ").append(dirPath).append('\n');
    }

    private void appendLog(BattleEnvironment env, String message) {
        String oldLog = env.getInstallLog() == null ? "" : env.getInstallLog();
        if (!oldLog.isEmpty() && !oldLog.endsWith("\n")) {
            oldLog = oldLog + "\n";
        }
        env.setInstallLog(oldLog + message + "\n");
    }

    private void runCommand(StringBuilder log, List<String> command, Path workingDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            if (workingDir != null) {
                pb.directory(workingDir.toFile());
            }
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append(line).append('\n');
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("命令执行失败: " + String.join(" ", command));
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private List<String> buildCondaCommand(String... args) {
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        if (isWindows && condaExecutable != null && condaExecutable.toLowerCase(Locale.ROOT).endsWith(".bat")) {
            ArrayList<String> cmd = new ArrayList<>();
            cmd.add("cmd");
            cmd.add("/c");
            cmd.add(condaExecutable);
            cmd.addAll(java.util.Arrays.asList(args));
            return cmd;
        }
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add(condaExecutable);
        cmd.addAll(java.util.Arrays.asList(args));
        return cmd;
    }

    private Path resolveEnvironmentRoot(String code) {
        String baseDir = (battleEnvRoot == null || battleEnvRoot.isBlank())
                ? Paths.get(System.getProperty("user.dir"), "battle_envs").toString()
                : battleEnvRoot;
        return Paths.get(baseDir, code);
    }

    private Path resolveWorkspaceRoot(Path envRoot) {
        if (defaultWorkspace != null && !defaultWorkspace.isBlank()) {
            return Paths.get(defaultWorkspace);
        }
        return envRoot.resolve("workspace");
    }

    private Path resolveCondaPythonPath(String envName) {
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        if (condaEnvsRoot != null && !condaEnvsRoot.isBlank()) {
            return isWindows
                    ? Paths.get(condaEnvsRoot, envName, "python.exe")
                    : Paths.get(condaEnvsRoot, envName, "bin", "python");
        }
        return isWindows
                ? Paths.get(envName, "python.exe")
                : Paths.get(envName, "bin", "python");
    }

    private String buildCondaEnvName(String code) {
        return "rl_battle_" + code + "_" + LocalDateTime.now().format(NAME_TIME);
    }

    private String buildDeletedCode(String originalCode, Integer id) {
        String safeCode = originalCode == null ? "deleted_env" : originalCode.trim();
        return safeCode + "__deleted__" + id + "_" + System.currentTimeMillis();
    }

    private String normalizeCode(String code) {
        return code.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }

    private Path resolveTeacherResourceRoot() {
        return Paths.get(uploadBaseDir, "teacher-resources").toAbsolutePath().normalize();
    }

    private TeacherUploadResource buildFailedResource(String name, MultipartFile file, String reason) {
        TeacherUploadResource resource = new TeacherUploadResource();
        resource.setName(name == null || name.isBlank() ? "未命名文件" : name.trim());
        String originalFilename = file == null ? "unknown_file" : sanitizeFilename(file.getOriginalFilename());
        resource.setOriginalFilename(originalFilename);
        resource.setStoredFilename(originalFilename);
        resource.setFilePath("FAILED:" + reason);
        resource.setContentType(file == null ? null : file.getContentType());
        resource.setFileSize(file == null ? 0L : file.getSize());
        resource.setStatus("FAILED");
        resource.setIsDeleted(false);
        resource.setCreateTime(LocalDateTime.now());
        resource.setUpdateTime(LocalDateTime.now());
        return resource;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed_file";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String safeZipName(String filename) {
        String value = filename == null || filename.isBlank() ? "environment.zip" : filename;
        return value.replaceAll("[\\r\\n]", "_");
    }

    private void addZipFile(ZipOutputStream zos, String pathValue, String entryName) throws IOException {
        if (pathValue == null || pathValue.isBlank()) {
            return;
        }
        Path path = Paths.get(pathValue);
        if (!Files.exists(path)) {
            return;
        }
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        Files.copy(path, zos);
        zos.closeEntry();
    }
}
