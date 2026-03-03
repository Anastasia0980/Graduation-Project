package com.rloj.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.nio.file.*;

@RestController
@CrossOrigin
public class uploadcontroller {

    private static final String BASE_UPLOAD_DIR = "uploads";

    private static final String PYTHON_PATH = "E:/Anaconda/envs/rl/python.exe";

    private static final String PYTHON_SCRIPT = "evaluator.py";

    @PostMapping("/upload")
    public ResponseEntity<String> handleUpload(
            @RequestParam("config") MultipartFile configFile,
            @RequestParam("model") MultipartFile modelFile,
            @RequestParam("studentType") String studentType
    ) {

        try {

            File baseDir = new File(BASE_UPLOAD_DIR);
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            int currentId = getCurrentFolderId(baseDir);

            File groupDir = new File(baseDir, String.valueOf(currentId));
            if (!groupDir.exists()) {
                groupDir.mkdirs();
            }

            File studentDir = new File(groupDir, studentType);
            studentDir.mkdirs();

            Path configPath = Paths.get(studentDir.getAbsolutePath(), "config.json");
            Path modelPath = Paths.get(studentDir.getAbsolutePath(), "model.pt");

            Files.write(configPath, configFile.getBytes());
            Files.write(modelPath, modelFile.getBytes());

            System.out.println("文件保存成功: " + studentDir.getAbsolutePath());

            File student1Dir = new File(groupDir, "student1");
            File student2Dir = new File(groupDir, "student2");

            File resultFile = new File(groupDir, "result.txt");

            if (student1Dir.exists() && student2Dir.exists()) {

                if (resultFile.exists()) {
                    return ResponseEntity.ok("该组已测评完成");
                }

                System.out.println("两个学生都已提交，开始测评...");

                String result = runPythonEvaluation(
                        student1Dir.getAbsolutePath(),
                        student2Dir.getAbsolutePath()
                );

                Files.write(
                        resultFile.toPath(),
                        result.getBytes(),
                        StandardOpenOption.CREATE
                );

                return ResponseEntity.ok("测评完成：\n" + result);
            }

            return ResponseEntity.ok("上传成功，当前组编号：" + currentId);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("上传失败：" + e.getMessage());
        }
    }

    // ===============================
    // 运行 Python 测评（同步）
    // ===============================
    private String runPythonEvaluation(String student1Path, String student2Path) {

        StringBuilder output = new StringBuilder();

        try {

            ProcessBuilder builder = new ProcessBuilder(
                    PYTHON_PATH,
                    PYTHON_SCRIPT,
                    student1Path,
                    student2Path
            );

            builder.directory(new File(".")); // backend 根目录
            builder.redirectErrorStream(true);

            Process process = builder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[PYTHON] " + line);
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            System.out.println("Python 运行结束，exit code = " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
            output.append("测评运行异常");
        }

        return output.toString();
    }

    // ===============================
    // 获取当前编号
    // ===============================
    private int getCurrentFolderId(File baseDir) {

        File[] files = baseDir.listFiles(File::isDirectory);

        int maxId = 0;

        if (files != null) {
            for (File file : files) {
                try {
                    int id = Integer.parseInt(file.getName());
                    if (id > maxId) {
                        maxId = id;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (maxId == 0) {
            return 1;
        }

        File latestDir = new File(baseDir, String.valueOf(maxId));

        File student1Dir = new File(latestDir, "student1");
        File student2Dir = new File(latestDir, "student2");

        if (student1Dir.exists() && student2Dir.exists()) {
            return maxId + 1;
        }

        return maxId;
    }
}