package org.example.rlplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderBoard {

    private Integer rank;
    private Integer studentId;
    private String nickname;

    /**
     * 分组对战扩展字段
     */
    private Long teamId;
    private String teamName;
    private String captainName;
    private String member1Name;
    private String member2Name;

    /**
     * 兼容旧字段；这里与 ladderScore 保持一致
     */
    private double bestScore;

    /**
     * 对战模式排行榜：天梯分
     */
    private Integer ladderScore;

    /**
     * 战绩：x胜y负z平（按一次挑战=一场统计）
     */
    private Integer winCount;
    private Integer loseCount;
    private Integer drawCount;

    /**
     * 对战场次（按一次挑战记一场，不是30局中的局数）
     */
    private Integer matchCount;
}
