package com.study.group.group.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    private String description;

    @NotNull(message = "정원은 필수입니다.")
    @Min(value = 2, message = "정원은 최소 2명 이상이어야 합니다.")
    @Max(value = 50, message = "정원은 최대 50명까지입니다.")
    private Integer maxMembers;

    @NotBlank(message = "모임 방식은 필수입니다.")
    private String meetingType;  // 온라인 / 오프라인 / 혼합

    private String region;  // 오프라인/혼합일 때 사용

    private String subject;

    private String ageRange;

    private String gender;  // 남 / 여 / 무관
}