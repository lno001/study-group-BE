package com.study.group.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private String loginId;
    private String nickname;
    private String gender;
    private Integer age;
    private String education;
    private String region;
    private String genderPublic;
    private String agePublic;
    private String educationPublic;
    private String regionPublic;
}