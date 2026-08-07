package com.study.group.auth.service;

import com.study.group.auth.dto.LoginRequest;
import com.study.group.auth.dto.RefreshRequest;
import com.study.group.auth.dto.SignupRequest;
import com.study.group.auth.dto.TokenResponse;
import com.study.group.auth.entity.RefreshToken;
import com.study.group.auth.jwt.JwtTokenProvider;
import com.study.group.auth.repository.RefreshTokenRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    // ========== 회원가입 ==========

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        SignupRequest request = new SignupRequest();
        request.setLoginId("lno001");
        request.setNickname("김복자");
        request.setPassword("1234");
        request.setGender("남");
        request.setAge(25);
        request.setEducation("고등학생");
        request.setRegion("1구");

        given(userRepository.existsByLoginId("lno001")).willReturn(false);
        given(userRepository.existsByNickname("김복자")).willReturn(false);
        given(passwordEncoder.encode("1234")).willReturn("encoding1234");

        authService.signup(request);

        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(passwordEncoder).encode("1234");
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 중복")
    void signup_duplicateLoginId() {
        SignupRequest request = new SignupRequest();
        request.setLoginId("lno001");
        request.setNickname("김복자");
        request.setPassword("1234");

        given(userRepository.existsByLoginId("lno001")).willReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.signup(request)
        );
        assertTrue(ex.getMessage().contains("아이디"));
        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void signup_duplicateNickname() {
        SignupRequest request = new SignupRequest();
        request.setLoginId("lno001");
        request.setNickname("김복자");
        request.setPassword("1234");

        given(userRepository.existsByLoginId("lno001")).willReturn(false);
        given(userRepository.existsByNickname("김복자")).willReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.signup(request)
        );
        assertTrue(ex.getMessage().contains("닉네임"));
        verify(userRepository, never()).save(any());
    }

    // ========== 로그인 ==========

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setLoginId("lno001");
        request.setPassword("1234");

        User user = User.builder()
                .userId(1L)
                .loginId("lno001")
                .nickname("김복자")
                .password("encoding1234")
                .role("USER")
                .isDeleted("N")
                .build();

        given(userRepository.findByLoginIdAndIsDeleted("lno001", "N"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("1234", "encoding1234")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L, "김복자", "USER"))
                .willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshTokenExpiration()).willReturn(1209600000L);
        given(refreshTokenRepository.findByUserAndIsDeleted(user, "N"))
                .willReturn(Optional.empty());

        TokenResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 실패 - 아이디 없음")
    void login_userNotFound() {
        LoginRequest request = new LoginRequest();
        request.setLoginId("nobody");
        request.setPassword("1234");

        given(userRepository.findByLoginIdAndIsDeleted("nobody", "N"))
                .willReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );
        assertTrue(ex.getMessage().contains("아이디 또는 비밀번호"));
        verify(jwtTokenProvider, never()).createAccessToken(any(), any(), any());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_wrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setLoginId("lno001");
        request.setPassword("wrong");

        User user = User.builder()
                .userId(1L)
                .loginId("lno001")
                .nickname("김복자")
                .password("encoding1234")
                .role("USER")
                .isDeleted("N")
                .build();

        given(userRepository.findByLoginIdAndIsDeleted("lno001", "N"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoding1234")).willReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );
        assertTrue(ex.getMessage().contains("아이디 또는 비밀번호"));
        verify(jwtTokenProvider, never()).createAccessToken(any(), any(), any());
    }

    // ========== 로그아웃 ==========

    @Test
    @DisplayName("로그아웃 성공 - 리프레시 토큰 삭제 처리")
    void logout_success() {
        User user = User.builder().userId(1L).loginId("lno001").build();
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .refreshToken("refresh-token")
                .isDeleted("N")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(refreshTokenRepository.findByUserAndIsDeleted(user, "N"))
                .willReturn(Optional.of(token));

        authService.logout(1L);

        assertEquals("Y", token.getIsDeleted());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("로그아웃 실패 - 없는 회원")
    void logout_userNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.logout(99L));
    }

    // ========== 토큰 재발급 ==========

    @Test
    @DisplayName("리프레시 성공")
    void refresh_success() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-token");

        User user = User.builder()
                .userId(1L)
                .nickname("김복자")
                .role("USER")
                .isDeleted("N")
                .build();

        RefreshToken saved = RefreshToken.builder()
                .user(user)
                .refreshToken("refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isDeleted("N")
                .build();

        given(jwtTokenProvider.validateToken("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByRefreshTokenAndIsDeleted("refresh-token", "N"))
                .willReturn(Optional.of(saved));
        given(jwtTokenProvider.createAccessToken(1L, "김복자", "USER"))
                .willReturn("new-access");

        TokenResponse response = authService.refresh(request);

        assertEquals("new-access", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    @DisplayName("리프레시 실패 - JWT 무효")
    void refresh_invalidJwt() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("bad");

        given(jwtTokenProvider.validateToken("bad")).willReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.refresh(request));
    }

    @Test
    @DisplayName("리프레시 실패 - DB에 토큰 없음")
    void refresh_tokenNotInDb() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-token");

        given(jwtTokenProvider.validateToken("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByRefreshTokenAndIsDeleted("refresh-token", "N"))
                .willReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.refresh(request));
    }

    @Test
    @DisplayName("리프레시 실패 - 만료된 토큰")
    void refresh_expired() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-token");

        User user = User.builder().userId(1L).isDeleted("N").build();
        RefreshToken saved = RefreshToken.builder()
                .user(user)
                .refreshToken("refresh-token")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .isDeleted("N")
                .build();

        given(jwtTokenProvider.validateToken("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByRefreshTokenAndIsDeleted("refresh-token", "N"))
                .willReturn(Optional.of(saved));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refresh(request)
        );
        assertTrue(ex.getMessage().contains("만료"));
        assertEquals("Y", saved.getIsDeleted());
    }

    @Test
    @DisplayName("리프레시 실패 - 탈퇴 회원")
    void refresh_deletedUser() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-token");

        User user = User.builder().userId(1L).isDeleted("Y").build();
        RefreshToken saved = RefreshToken.builder()
                .user(user)
                .refreshToken("refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isDeleted("N")
                .build();

        given(jwtTokenProvider.validateToken("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByRefreshTokenAndIsDeleted("refresh-token", "N"))
                .willReturn(Optional.of(saved));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refresh(request)
        );
        assertTrue(ex.getMessage().contains("탈퇴"));
    }
}