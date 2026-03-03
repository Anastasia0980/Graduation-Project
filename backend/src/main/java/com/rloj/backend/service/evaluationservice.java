package com.rloj.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class evaluationservice {

    @Autowired
    private pythonrunnerservice pythonRunnerService;

    public void tryEvaluate(String roundPath) {

        File student1 = new File(roundPath + "/student1");
        File student2 = new File(roundPath + "/student2");

        if (student1.exists() && student2.exists()) {

            System.out.println("两个学生已提交，开始测评");

            pythonRunnerService.runEvaluation(
                    student1.getAbsolutePath(),
                    student2.getAbsolutePath()
            );

        } else {
            System.out.println("等待另一方提交");
        }
    }
}