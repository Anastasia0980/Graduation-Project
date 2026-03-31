package org.example.rlplatform.vo;

import lombok.Data;

@Data
public class ScoreExportRowVO {

    private Integer rank;
    private String name;

    // 单人模式
    private Integer levelCount;
    private String clearTime;

    // 对战模式
    private Integer ladderScore;
    private Integer matchCount;
    private Integer winCount;
    private Integer loseCount;
    private Integer drawCount;
}