package com.study.group.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.study.group.user.dto.UserResponse;
import com.study.group.user.dto.UserUpdateRequest;
import com.study.group.user.entity.User;
import com.study.group.user.entity.UserProfile;
import com.study.group.user.repository.UserProfileRepository;
import com.study.group.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if ("Y".equals(user.getIsDeleted())) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필 정보가 없습니다."));

        return UserResponse.builder()
                .loginId(user.getLoginId())
                .nickname(user.getNickname())
                .gender(profile.getGender())
                .age(profile.getAge())
                .education(profile.getEducation())
                .region(profile.getRegion())
                .genderPublic(profile.getGenderPublic())
                .agePublic(profile.getAgePublic())
                .educationPublic(profile.getEducationPublic())
                .regionPublic(profile.getRegionPublic())
                .build();
    }

    // 내 정보 수정
    @Transactional
    public UserResponse updateMyInfo(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if ("Y".equals(user.getIsDeleted())) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 닉네임 변경
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
            user.setNickname(request.getNickname());
        }

        // 비밀번호 변경 (선택)
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필 정보가 없습니다."));

        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getAge() != null) profile.setAge(request.getAge());
        if (request.getEducation() != null) profile.setEducation(request.getEducation());
        if (request.getRegion() != null) profile.setRegion(request.getRegion());
        if (request.getGenderPublic() != null) profile.setGenderPublic(request.getGenderPublic());
        if (request.getAgePublic() != null) profile.setAgePublic(request.getAgePublic());
        if (request.getEducationPublic() != null) profile.setEducationPublic(request.getEducationPublic());
        if (request.getRegionPublic() != null) profile.setRegionPublic(request.getRegionPublic());

        return getMyInfo(userId);
    }

    // 회원 탈퇴 (소프트 삭제)
    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if ("Y".equals(user.getIsDeleted())) {
            throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
        }

        user.setIsDeleted("Y");
    }
}