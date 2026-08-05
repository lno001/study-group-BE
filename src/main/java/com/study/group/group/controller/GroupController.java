package com.study.group.group.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.study.group.common.ApiResponse;
import com.study.group.common.PageResponse;
import com.study.group.group.dto.GroupCreateRequest;
import com.study.group.group.dto.GroupResponse;
import com.study.group.group.dto.GroupUpdateRequest;
import com.study.group.group.service.GroupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // 그룹 생성
    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            Authentication authentication,
            @Valid @RequestBody GroupCreateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        GroupResponse response = groupService.createGroup(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "스터디 그룹이 생성되었습니다.", response));
    }

 // 그룹 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<GroupResponse>>> getGroups(
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<GroupResponse> groups = groupService.getGroups(region, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(200, "그룹 목록 조회 성공", groups));
    }

 // 상세 조회
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroup(
            @PathVariable("groupId") Long groupId
    ) {
        GroupResponse response = groupService.getGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success(200, "그룹 상세 조회 성공", response));
    }

    // 수정
    @PutMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @Valid @RequestBody GroupUpdateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        GroupResponse response = groupService.updateGroup(userId, groupId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "그룹 수정 성공", response));
    }

    // 삭제
    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            Authentication authentication,
            @PathVariable("groupId") Long groupId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        groupService.deleteGroup(userId, groupId);
        return ResponseEntity.ok(ApiResponse.success(200, "그룹이 삭제되었습니다."));
    }
}