package org.example.rlplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内存排队用的提交对象（不入库）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    private Long studentId;
    private String environment;
    private Integer games;

    /** 相对 workspace 的学生提交目录：uploads/<studentId>/<uuid> */
    private String studentDirRel;
}