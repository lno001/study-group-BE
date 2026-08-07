package com.study.group.user.service;

import com.study.group.user.dto.UserResponse;
import com.study.group.user.dto.UserUpdateRequest;
import com.study.group.user.entity.User;
import com.study.group.user.entity.UserProfile;
import com.study.group.user.repository.UserProfileRepository;
import com.study.group.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user(Long id) {
        return User.builder()
                .userId(id)
                .loginId("lno001")
                .nickname("김복자")
                .password("encoded-old")
                .role("USER")
                .isDeleted("N")
                .build();
    }

    private UserProfile profile(Long userId) {
        return UserProfile.builder()
                .userId(userId)
                .gender("남")
                .age(25)
                .education("대학생")
                .region("1구")
                .genderPublic("N")
                .agePublic("N")
                .educationPublic("N")
                .regionPublic("Y")
                .build();
    }

    // ===== 조회 =====

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyInfo_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(userProfileRepository.findById(1L)).willReturn(Optional.of(profile(1L)));

        UserResponse res = userService.getMyInfo(1L);

        assertEquals("lno001", res.getLoginId());
        assertEquals("김복자", res.getNickname());
        assertEquals("1구", res.getRegion());
        assertEquals("Y", res.getRegionPublic());
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 회원 없음")
    void getMyInfo_userNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.getMyInfo(99L));
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 탈퇴 회원")
    void getMyInfo_deleted() {
        User deleted = user(1L);
        deleted.setIsDeleted("Y");
        given(userRepository.findById(1L)).willReturn(Optional.of(deleted));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.getMyInfo(1L));
        assertTrue(ex.getMessage().contains("탈퇴"));
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 프로필 없음")
    void getMyInfo_noProfile() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(userProfileRepository.findById(1L)).willReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.getMyInfo(1L));
        assertTrue(ex.getMessage().contains("프로필"));
    }

    // ===== 수정 =====

    @Test
    @DisplayName("내 정보 수정 성공 - 닉네임·프로필")
    void updateMyInfo_success() {
        User u = user(1L);
        UserProfile p = profile(1L);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setCurrentPassword("old-pw");
        req.setNickname("새닉네임");
        req.setRegion("2구");
        req.setGenderPublic("Y");

        given(userRepository.findById(1L)).willReturn(Optional.of(u));
        given(passwordEncoder.matches("old-pw", "encoded-old")).willReturn(true);
        given(userRepository.existsByNickname("새닉네임")).willReturn(false);
        given(userProfileRepository.findById(1L)).willReturn(Optional.of(p));
        // getMyInfo 재호출용
        given(userRepository.findById(1L)).willReturn(Optional.of(u));
        given(userProfileRepository.findById(1L)).willReturn(Optional.of(p));

        UserResponse res = userService.updateMyInfo(1L, req);

        assertEquals("새닉네임", u.getNickname());
        assertEquals("2구", p.getRegion());
        assertEquals("Y", p.getGenderPublic());
        assertEquals("새닉네임", res.getNickname());
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 현재 비밀번호 틀림")
    void updateMyInfo_wrongPassword() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setCurrentPassword("wrong");
        req.setNickname("아무개");

        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(passwordEncoder.matches("wrong", "encoded-old")).willReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateMyInfo(1L, req));
        assertTrue(ex.getMessage().contains("비밀번호"));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 닉네임 중복")
    void updateMyInfo_duplicateNickname() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setCurrentPassword("old-pw");
        req.setNickname("이미있음");

        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(passwordEncoder.matches("old-pw", "encoded-old")).willReturn(true);
        given(userRepository.existsByNickname("이미있음")).willReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateMyInfo(1L, req));
        assertTrue(ex.getMessage().contains("닉네임"));
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 비밀번호 변경")
    void updateMyInfo_changePassword() {
        User u = user(1L);
        UserProfile p = profile(1L);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setCurrentPassword("old-pw");
        req.setNewPassword("new-pw");

        given(userRepository.findById(1L)).willReturn(Optional.of(u));
        given(passwordEncoder.matches("old-pw", "encoded-old")).willReturn(true);
        given(passwordEncoder.encode("new-pw")).willReturn("encoded-new");
        given(userProfileRepository.findById(1L)).willReturn(Optional.of(p));

        userService.updateMyInfo(1L, req);

        assertEquals("encoded-new", u.getPassword());
        verify(passwordEncoder).encode("new-pw");
    }

    @Test
    @DisplayName("닉네임 동일하면 중복 체크 안 함")
    void updateMyInfo_sameNickname_skipDuplicateCheck() {
        User u = user(1L);
        UserProfile p = profile(1L);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setCurrentPassword("old-pw");
        req.setNickname("김복자"); // 기존과 동일

        given(userRepository.findById(1L)).willReturn(Optional.of(u));
        given(passwordEncoder.matches("old-pw", "encoded-old")).willReturn(true);
        given(userProfileRepository.findById(1L)).willReturn(Optional.of(p));

        userService.updateMyInfo(1L, req);

        verify(userRepository, never()).existsByNickname(anyString());
    }

    // ===== 탈퇴 =====

    @Test
    @DisplayName("회원 탈퇴 성공 - 소프트 삭제")
    void withdraw_success() {
        User u = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(u));

        userService.withdraw(1L);

        assertEquals("Y", u.getIsDeleted());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 이미 탈퇴")
    void withdraw_alreadyDeleted() {
        User u = user(1L);
        u.setIsDeleted("Y");
        given(userRepository.findById(1L)).willReturn(Optional.of(u));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.withdraw(1L));
        assertTrue(ex.getMessage().contains("이미 탈퇴"));
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 회원 없음")
    void withdraw_notFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.withdraw(99L));
    }
}