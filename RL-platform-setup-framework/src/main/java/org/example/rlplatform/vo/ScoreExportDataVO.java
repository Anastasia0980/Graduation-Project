package org.example.rlplatform.vo;

import lombok.Data;

import java.util.List;

@Data
public class ScoreExportDataVO {

    private Integer assignmentId;
    private String className;
    private String taskName;
    private String mode;
    private List<ScoreExportRowVO> rows;
}