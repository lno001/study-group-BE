package com.study.group.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberDecideRequest {

    // 수락 / 거절
    @NotBlank(message = "처리 상태는 필수입니다.")
    private String status;
}