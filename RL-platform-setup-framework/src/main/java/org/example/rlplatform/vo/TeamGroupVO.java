package org.example.rlplatform.vo;

import lombok.Data;

import java.util.List;

@Data
public class TeamGroupVO {

    private Integer teamId;
    private String teamName;
    private String teamCode;

    private Integer captainId;
    private String captainName;

    private List<String> memberNames;
    private Integer memberCount;
    private Integer maxMembers;

    private Boolean captain;
    private Boolean locked;
    private String lockTime;
}
