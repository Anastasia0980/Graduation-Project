package org.example.rlplatform.vo;

import lombok.Data;

@Data
public class ScoreExportRowVO {

    private Integer rank;
    private String name;

    // 分组对战模式扩展
    private String captainName;
    private String member1Name;
    private String member2Name;

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
