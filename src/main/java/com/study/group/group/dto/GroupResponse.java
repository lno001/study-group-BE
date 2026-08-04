package com.study.group.group.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class GroupResponse {

    private Long groupId;
    private String title;
    private String description;
    private Integer maxMembers;
    private Integer currentMembers;
    private String status;
    private String meetingType;
    private String region;
    private String subject;
    private String ageRange;
    private String gender;
    private String leaderNickname;
    private LocalDateTime createdAt;
}