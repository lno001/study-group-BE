package com.study.group.group.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.study.group.common.PageResponse;
import com.study.group.group.dto.GroupCreateRequest;
import com.study.group.group.dto.GroupResponse;
import com.study.group.group.entity.StudyGroup;
import com.study.group.group.repository.StudyGroupRepository;
import com.study.group.user.entity.User;
import com.study.group.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final UserRepository userRepository;

    // 그룹 생성
    @Transactional
    public GroupResponse createGroup(Long leaderId, GroupCreateRequest request) {
        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if ("Y".equals(leader.getIsDeleted())) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        // 오프라인/혼합인데 지역이 없으면 체크 (선택)
        if (!"온라인".equals(request.getMeetingType())
                && (request.getRegion() == null || request.getRegion().isBlank())) {
            throw new IllegalArgumentException("오프라인/혼합 모임은 지역을 입력해야 합니다.");
        }

        StudyGroup group = StudyGroup.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .maxMembers(request.getMaxMembers())
                .currentMembers(1)
                .status("모집중")
                .meetingType(request.getMeetingType())
                .region(request.getRegion())
                .subject(request.getSubject())
                .ageRange(request.getAgeRange())
                .gender(request.getGender() != null ? request.getGender() : "무관")
                .leader(leader)
                .isDeleted("N")
                .build();

        studyGroupRepository.save(group);

        return toResponse(group);
    }

    private GroupResponse toResponse(StudyGroup group) {
        return GroupResponse.builder()
                .groupId(group.getGroupId())
                .title(group.getTitle())
                .description(group.getDescription())
                .maxMembers(group.getMaxMembers())
                .currentMembers(group.getCurrentMembers())
                .status(group.getStatus())
                .meetingType(group.getMeetingType())
                .region(group.getRegion())
                .subject(group.getSubject())
                .ageRange(group.getAgeRange())
                .gender(group.getGender())
                .leaderNickname(group.getLeader().getNickname())
                .createdAt(group.getCreatedAt())
                .build();
    }
    
 // 그룹 목록 조회 (삭제되지 않은 것만)
    @Transactional(readOnly = true)
    public PageResponse<GroupResponse> getGroups(String region, String status, Pageable pageable) {
        Page<StudyGroup> page;

        if (region != null && !region.isBlank() && status != null && !status.isBlank()) {
            page = studyGroupRepository.findByRegionAndStatusAndIsDeleted(region, status, "N", pageable);
        } else if (region != null && !region.isBlank()) {
            page = studyGroupRepository.findByRegionAndIsDeleted(region, "N", pageable);
        } else if (status != null && !status.isBlank()) {
            page = studyGroupRepository.findByStatusAndIsDeleted(status, "N", pageable);
        } else {
            page = studyGroupRepository.findByIsDeleted("N", pageable);
        }

        Page<GroupResponse> responsePage = page.map(this::toResponse);
        return PageResponse.of(responsePage);
    }
}