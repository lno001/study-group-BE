package com.study.group.member.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponse {

    private Long memberId;
    private Long groupId;
    private String groupTitle;
    private Long userId;
    private String nickname;
    private String status;      // 신청 / 수락 / 거절
    private LocalDateTime joinedAt;

    // 그룹장이 볼 때 공개 설정된 프로필 (선택)
    private String gender;
    private Integer age;
    private String education;
    private String region;
}