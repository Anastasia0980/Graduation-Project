package org.example.rlplatform.vo;

import lombok.Data;

@Data
public class BattleModelOptionVO {
    private Long submissionId;
    private Long studentId;
    private String studentName;
    private String modelName;
    private String submitTime;
    private Integer winCount;
    private Integer loseCount;
    private Integer drawCount;
}