package com.study.group.group.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.study.group.common.PageResponse;
import com.study.group.group.dto.GroupCreateRequest;
import com.study.group.group.dto.GroupResponse;
import com.study.group.group.dto.GroupUpdateRequest;
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

        validateMeetingAndRegion(request.getMeetingType(), request.getRegion());

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
                .isPublic(
                	    request.getIsPublic() != null && !request.getIsPublic().isBlank()
                	        ? request.getIsPublic()
                	        : "Y"
                	)
                .build();

        studyGroupRepository.save(group);
        return toResponse(group);
    }

    // 그룹 목록 (페이징)
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

        return PageResponse.of(page.map(this::toResponse));
    }

    // 그룹 상세
    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long groupId) {
        StudyGroup group = studyGroupRepository.findByGroupIdAndIsDeleted(groupId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디 그룹입니다."));
        return toResponse(group);
    }

    // 그룹 수정 (개설자만, 전체 폼 제출 기준)
    @Transactional
    public GroupResponse updateGroup(Long userId, Long groupId, GroupUpdateRequest request) {
        StudyGroup group = studyGroupRepository.findByGroupIdAndIsDeleted(groupId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디 그룹입니다."));

        if (!group.getLeader().getUserId().equals(userId)) {
            throw new IllegalArgumentException("그룹 수정 권한이 없습니다.");
        }

        if (request.getMaxMembers() < group.getCurrentMembers()) {
            throw new IllegalArgumentException("정원은 현재 인원보다 적을 수 없습니다.");
        }

        validateMeetingAndRegion(request.getMeetingType(), request.getRegion());

        group.setTitle(request.getTitle());
        group.setDescription(request.getDescription());
        group.setMaxMembers(request.getMaxMembers());
        group.setMeetingType(request.getMeetingType());
        group.setRegion(request.getRegion());
        group.setSubject(request.getSubject());
        group.setAgeRange(request.getAgeRange());
        group.setGender(request.getGender());
        group.setStatus(request.getStatus());
        group.setIsPublic(request.getIsPublic());

        return toResponse(group);
    }

    // 그룹 삭제 (개설자만, 소프트 삭제)
    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        StudyGroup group = studyGroupRepository.findByGroupIdAndIsDeleted(groupId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디 그룹입니다."));

        if (!group.getLeader().getUserId().equals(userId)) {
            throw new IllegalArgumentException("그룹 삭제 권한이 없습니다.");
        }

        group.setIsDeleted("Y");
    }

    // 온/오프라인과 지역 관계 검증
    private void validateMeetingAndRegion(String meetingType, String region) {
        if (!"온라인".equals(meetingType)
                && (region == null || region.isBlank())) {
            throw new IllegalArgumentException("오프라인/혼합 모임은 지역을 입력해야 합니다.");
        }
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
                .isPublic(group.getIsPublic())
                .build();
    }
}