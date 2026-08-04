package com.study.group.user.controller;

import com.study.group.common.ApiResponse;
import com.study.group.user.dto.UserResponse;
import com.study.group.user.dto.UserUpdateRequest;
import com.study.group.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserResponse response = userService.getMyInfo(userId);
        return ResponseEntity.ok(ApiResponse.success(200, "내 정보 조회 성공", response));
    }

    // 내 정보 수정
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        UserResponse response = userService.updateMyInfo(userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "내 정보 수정 성공", response));
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userService.withdraw(userId);
        return ResponseEntity.ok(ApiResponse.success(200, "회원 탈퇴가 완료되었습니다."));
    }
}