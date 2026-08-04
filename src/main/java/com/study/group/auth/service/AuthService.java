package com.study.group.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
    @Transactional
    public void signup(SignupRequest request) {
    	if (userRepository.existsByLoginId(request.getLoginId())) {
    	    throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
    	}
    	if (userRepository.existsByNickname(request.getNickname())) {
    	    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
    	}

    	User user = User.builder()
    	        .loginId(request.getLoginId())
    	        .nickname(request.getNickname())
    	        .password(passwordEncoder.encode(request.getPassword()))
    	        .role("USER")
    	        .isDeleted("N")
    	        .build();

        userRepository.save(user);

        // 프로필 생성
        UserProfile profile = UserProfile.builder()
                .user(user)
                .gender(request.getGender())
                .age(request.getAge())
                .education(request.getEducation())
                .region(request.getRegion())
                .genderPublic("N")
                .agePublic("N")
                .educationPublic("N")
                .regionPublic("Y")
                .build();

        userProfileRepository.save(profile);
    }

    // 로그인
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByLoginIdAndIsDeleted(request.getLoginId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(), user.getNickname(), user.getRole()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // 기존 리프레시 토큰 삭제 처리 (선택)
        refreshTokenRepository.findByUserAndIsDeleted(user, "N")
                .ifPresent(token -> {
                    token.setIsDeleted("Y");
                    refreshTokenRepository.save(token);
                });

        // 새 리프레시 토큰 저장
        RefreshToken tokenEntity = RefreshToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(
                        jwtTokenProvider.getRefreshTokenExpiration() / 1000
                ))
                .isDeleted("N")
                .build();

        refreshTokenRepository.save(tokenEntity);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
    
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        refreshTokenRepository.findByUserAndIsDeleted(user, "N")
                .ifPresent(token -> {
                    token.setIsDeleted("Y");
                    refreshTokenRepository.save(token);
                });
    }
    
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. JWT 자체 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        // 2. DB에서 토큰 확인 (로그아웃/삭제 여부)
        RefreshToken savedToken = refreshTokenRepository
                .findByRefreshTokenAndIsDeleted(refreshToken, "N")
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        // 3. 만료 시간 확인
        if (savedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            savedToken.setIsDeleted("Y");
            refreshTokenRepository.save(savedToken);
            throw new IllegalArgumentException("만료된 토큰입니다.");
        }

        User user = savedToken.getUser();

        if ("Y".equals(user.getIsDeleted())) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        // 4. 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(), user.getNickname(), user.getRole()
        );

        // 5. Refresh Token은 그대로 반환 (또는 아래처럼 재발급 가능)
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}