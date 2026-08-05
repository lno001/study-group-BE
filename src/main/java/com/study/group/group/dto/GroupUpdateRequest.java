package com.study.group.group.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupUpdateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100)
    private String title;

    @Size(max = 1000)
    private String description;  // 선택이면 @NotBlank 없이

    @NotNull(message = "정원은 필수입니다.")
    @Min(2) @Max(50)
    private Integer maxMembers;

    @NotBlank(message = "모임 방식은 필수입니다.")
    private String meetingType;

    private String region;  // 온라인일 수 있어서 선택

    private String subject;
    private String ageRange;

    @NotBlank(message = "성별 조건은 필수입니다.")
    private String gender;

    @NotBlank(message = "상태는 필수입니다.")
    private String status;

    @NotBlank(message = "공개 여부는 필수입니다.")
    private String isPublic;
}