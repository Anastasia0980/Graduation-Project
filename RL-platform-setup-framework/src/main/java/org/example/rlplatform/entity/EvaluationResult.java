package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="evaluation_result")
public class EvaluationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(nullable = false)
    private Integer result; // 0-成功  1-失败

    @Column
    private Integer winner; //当particpant=null时（即单人模式下与baseline比较时），winner=1表示学生获胜，winner=0表示学生落败

    @Column(name = "detailed_results", columnDefinition = "JSON")
    private String detailedResults;

    @Column(name = "result_dir", length = 500)
    private String resultDir;
}
