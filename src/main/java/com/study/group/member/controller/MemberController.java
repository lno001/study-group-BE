package com.study.group.member.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.study.group.common.ApiResponse;
import com.study.group.member.dto.MemberDecideRequest;
import com.study.group.member.dto.MemberResponse;
import com.study.group.member.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 스터디 신청
    @PostMapping("/api/groups/{groupId}/members")
    public ResponseEntity<ApiResponse<MemberResponse>> apply(
            Authentication authentication,
            @PathVariable("groupId") Long groupId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        MemberResponse response = memberService.apply(userId, groupId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "스터디 신청이 완료되었습니다.", response));
    }

    // 신청 허가 / 거절
    @PatchMapping("/api/groups/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<MemberResponse>> decide(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @PathVariable("memberId") Long memberId,
            @Valid @RequestBody MemberDecideRequest request
    ) {
        Long leaderId = (Long) authentication.getPrincipal();
        MemberResponse response = memberService.decide(leaderId, groupId, memberId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "신청이 처리되었습니다.", response));
    }

 // 멤버 목록 조회
    @GetMapping("/api/groups/{groupId}/members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getMembers(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @RequestParam(value = "status", required = false) String status
    ) {
        Long userId = (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Long)
                ? (Long) authentication.getPrincipal()
                : null;

        List<MemberResponse> members = memberService.getMembers(userId, groupId, status);
        return ResponseEntity.ok(ApiResponse.success(200, "멤버 목록 조회 성공", members));
    }
    
    @GetMapping("/api/groups/{groupId}/members/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> myStatus(
            Authentication authentication,
            @PathVariable("groupId") Long groupId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String status = memberService.getMyStatus(userId, groupId);
        return ResponseEntity.ok(
                ApiResponse.success(200, "조회 성공",
                        Map.of("status", status == null ? "" : status))
        );
    }

 // 자발적 탈퇴
    @DeleteMapping("/api/groups/{groupId}/members/me")
    public ResponseEntity<ApiResponse<Void>> leave(
            Authentication authentication,
            @PathVariable("groupId") Long groupId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        memberService.leave(userId, groupId);
        return ResponseEntity.ok(ApiResponse.success(200, "스터디에서 탈퇴했습니다."));
    }

    // 강퇴 (그룹장)
    @DeleteMapping("/api/groups/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> kick(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @PathVariable("memberId") Long memberId
    ) {
        Long leaderId = (Long) authentication.getPrincipal();
        memberService.kick(leaderId, groupId, memberId);
        return ResponseEntity.ok(ApiResponse.success(200, "멤버를 강퇴했습니다."));
    }
}