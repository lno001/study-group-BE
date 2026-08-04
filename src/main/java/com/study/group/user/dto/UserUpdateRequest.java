package com.study.group.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword;

    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영어, 숫자만 사용할 수 있습니다.")
    private String nickname;

    @Size(min = 4, max = 30, message = "비밀번호는 4~30자여야 합니다.")
    private String newPassword;  // 비밀번호 변경 시에만 입력

    private String gender;
    private Integer age;
    private String education;
    private String region;

    private String genderPublic;
    private String agePublic;
    private String educationPublic;
    private String regionPublic;
}