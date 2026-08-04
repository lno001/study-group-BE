package com.study.group.auth.repository;

import com.study.group.auth.entity.RefreshToken;
import com.study.group.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 유효한 리프레시 토큰 조회
    Optional<RefreshToken> findByRefreshTokenAndIsDeleted(String refreshToken, String isDeleted);

    // 특정 유저의 토큰 조회
    Optional<RefreshToken> findByUserAndIsDeleted(User user, String isDeleted);
}