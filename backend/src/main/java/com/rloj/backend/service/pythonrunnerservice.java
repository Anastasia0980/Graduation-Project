package com.rloj.backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class pythonrunnerservice {

    public void runEvaluation(String student1Path, String student2Path) {
        try {

            String pythonExe = "E:/Anaconda/envs/rl/python.exe"; // 如果用虚拟环境改成绝对路径
            String scriptPath = "evaluator.py"; // 放在 backend 根目录

            ProcessBuilder builder = new ProcessBuilder(
                    pythonExe,
                    scriptPath,
                    student1Path,
                    student2Path
            );

            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[PYTHON] " + line);
            }

            int exitCode = process.waitFor();
            System.out.println("Python finished: " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}