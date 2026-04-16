package org.example.rlplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskOverviewVO {

    private Integer assignmentId;
    private String taskName;
    private String className;
    private LocalDateTime deadline;
    private String status;

    private Integer submittedCount;
    private Integer unsubmittedCount;
    private Integer totalSubmissionCount;

    private List<String> submittedStudentNames;
    private List<String> unsubmittedStudentNames;
}