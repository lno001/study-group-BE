package com.study.group.member.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.study.group.group.entity.StudyGroup;
import com.study.group.group.repository.StudyGroupRepository;
import com.study.group.member.dto.MemberDecideRequest;
import com.study.group.member.dto.MemberResponse;
import com.study.group.member.entity.StudyMember;
import com.study.group.member.repository.StudyMemberRepository;
import com.study.group.user.entity.User;
import com.study.group.user.repository.UserProfileRepository;
import com.study.group.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    // 스터디 신청
    @Transactional
    public MemberResponse apply(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if ("Y".equals(user.getIsDeleted())) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        StudyGroup group = studyGroupRepository.findByGroupIdAndIsDeleted(groupId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디 그룹입니다."));

        if (!"모집중".equals(group.getStatus())) {
            throw new IllegalArgumentException("모집이 마감된 스터디입니다.");
        }

        if (group.getLeader().getUserId().equals(userId)) {
            throw new IllegalArgumentException("개설자는 신청할 수 없습니다.");
        }

        if (group.getCurrentMembers() >= group.getMaxMembers()) {
            throw new IllegalArgumentException("정원이 가득 찼습니다.");
        }

        // 이미 신청/수락된 적 있는지
        studyMemberRepository.findByGroupAndUserAndIsDeleted(group, user, "N")
                .ifPresent(m -> {
                    throw new IllegalArgumentException("이미 신청했거나 참여 중인 스터디입니다.");
                });

        StudyMember member = StudyMember.builder()
                .group(group)
                .user(user)
                .status("신청")
                .isDeleted("N")
                .build();

        studyMemberRepository.save(member);
        return toResponse(member, false);
    }

    private MemberResponse toResponse(StudyMember member, boolean includeProfile) {
        MemberResponse.MemberResponseBuilder builder = MemberResponse.builder()
                .memberId(member.getMemberId())
                .groupId(member.getGroup().getGroupId())
                .groupTitle(member.getGroup().getTitle())
                .userId(member.getUser().getUserId())
                .nickname(member.getUser().getNickname())
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt());

        if (includeProfile) {
            userProfileRepository.findById(member.getUser().getUserId())
                    .ifPresent(profile -> {
                        if ("Y".equals(profile.getGenderPublic())) {
                            builder.gender(profile.getGender());
                        }
                        if ("Y".equals(profile.getAgePublic())) {
                            builder.age(profile.getAge());
                        }
                        if ("Y".equals(profile.getEducationPublic())) {
                            builder.education(profile.getEducation());
                        }
                        if ("Y".equals(profile.getRegionPublic())) {
                            builder.region(profile.getRegion());
                        }
                    });
        }

        return builder.build();
    }

 // 신청 허가 / 거절 (그룹장만)
    @Transactional
    public MemberResponse decide(Long leaderId, Long groupId, Long memberId, MemberDecideRequest request) {
        StudyGroup group = studyGroupRepository.findByGroupIdAndIsDeleted(groupId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디 그룹입니다."));

        if (!group.getLeader().getUserId().equals(leaderId)) {
            throw new IllegalArgumentException("신청을 처리할 권한이 없습니다.");
        }

        StudyMember member = studyMemberRepository.findByMemberIdAndIsDeleted(memberId, "N")
                .orElseThrow(() -> new IllegalArgumentException("신청 내역이 없습니다."));

        if (!member.getGroup().getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("해당 그룹의 신청이 아닙니다.");
        }

        if (!"신청".equals(member.getStatus())) {
            throw new IllegalArgumentException("이미 처리된 신청입니다.");
        }

        String decide = request.getStatus();
        if (!"수락".equals(decide) && !"거절".equals(decide)) {
            throw new IllegalArgumentException("상태는 수락 또는 거절만 가능합니다.");
        }

        if ("수락".equals(decide)) {
            if (group.getCurrentMembers() >= group.getMaxMembers()) {
                throw new IllegalArgumentException("정원이 가득 찼습니다.");
            }

            member.setStatus("수락");
            group.setCurrentMembers(group.getCurrentMembers() + 1);

            // 정원 다 차면 자동 마감
            if (group.getCurrentMembers() >= group.getMaxMembers()) {
                group.setStatus("마감");
            }
        } else {
            member.setStatus("거절");
        }

        return toResponse(member, true);  // 그룹장에게는 공개 프로필 포함
    }

 // 그룹 멤버 목록
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(Long requesterId, Long groupId, String status) {
        StudyGroup group = studyGroupRepository.findByGroupIdAndIsDeleted(groupId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디 그룹입니다."));

        boolean isLeader = group.getLeader().getUserId().equals(requesterId);

        // 요청자가 수락 멤버인지
        boolean isAcceptedMember = false;
        if (requesterId != null) {
            isAcceptedMember = userRepository.findById(requesterId)
                    .flatMap(user -> studyMemberRepository.findByGroupAndUserAndIsDeleted(group, user, "N"))
                    .map(m -> "수락".equals(m.getStatus()))
                    .orElse(false);
        }

        boolean canSeeMembers = "Y".equals(group.getIsPublic()) || isLeader || isAcceptedMember;

        // 신청 목록은 그룹장만
        if (status != null && "신청".equals(status) && !isLeader) {
            throw new IllegalArgumentException("신청 목록은 그룹장만 볼 수 있습니다.");
        }

        // 비공개 + 외부인 → 차단
        if (!canSeeMembers) {
            throw new IllegalArgumentException("비공개 그룹의 멤버는 볼 수 없습니다.");
        }

        List<StudyMember> members;
        if (status != null && !status.isBlank()) {
            members = studyMemberRepository.findByGroupAndStatusAndIsDeleted(group, status, "N");
        } else {
            members = studyMemberRepository.findByGroupAndIsDeleted(group, "N");
        }

        // 그룹장이 아니면 수락만
        if (!isLeader) {
            members = members.stream()
                    .filter(m -> "수락".equals(m.getStatus()))
                    .toList();
        }

        // 목록에서는 닉네임 위주 (프로필은 그룹장만)
        return members.stream()
                .map(m -> toResponse(m, isLeader))
                .toList();
    }

 // 자발적 탈퇴 (본인)
    @Transactional
    public void leave(Long userId, Long groupId) {
        StudyGroup group = studyGroupRepository.findByGroupIdAndIsDeleted(groupId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디 그룹입니다."));

        if (group.getLeader().getUserId().equals(userId)) {
            throw new IllegalArgumentException("개설자는 탈퇴할 수 없습니다. 그룹을 삭제하세요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        StudyMember member = studyMemberRepository.findByGroupAndUserAndIsDeleted(group, user, "N")
                .orElseThrow(() -> new IllegalArgumentException("참여 중이 아닌 스터디입니다."));

        if (!"수락".equals(member.getStatus()) && !"신청".equals(member.getStatus())) {
            throw new IllegalArgumentException("탈퇴할 수 없는 상태입니다.");
        }

        // 수락 상태였으면 인원 감소
        if ("수락".equals(member.getStatus())) {
            group.setCurrentMembers(Math.max(1, group.getCurrentMembers() - 1));
            // 마감이었다면 다시 모집중으로
            if ("마감".equals(group.getStatus())
                    && group.getCurrentMembers() < group.getMaxMembers()) {
                group.setStatus("모집중");
            }
        }

        member.setIsDeleted("Y");
    }

    // 강퇴 (그룹장만)
    @Transactional
    public void kick(Long leaderId, Long groupId, Long memberId) {
        StudyGroup group = studyGroupRepository.findByGroupIdAndIsDeleted(groupId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디 그룹입니다."));

        if (!group.getLeader().getUserId().equals(leaderId)) {
            throw new IllegalArgumentException("강퇴 권한이 없습니다.");
        }

        StudyMember member = studyMemberRepository.findByMemberIdAndIsDeleted(memberId, "N")
                .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다."));

        if (!member.getGroup().getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("해당 그룹의 멤버가 아닙니다.");
        }

        if (member.getUser().getUserId().equals(leaderId)) {
            throw new IllegalArgumentException("자기 자신은 강퇴할 수 없습니다.");
        }

        if ("수락".equals(member.getStatus())) {
            group.setCurrentMembers(Math.max(1, group.getCurrentMembers() - 1));
            if ("마감".equals(group.getStatus())
                    && group.getCurrentMembers() < group.getMaxMembers()) {
                group.setStatus("모집중");
            }
        }

        member.setIsDeleted("Y");
    }
    
    @Transactional(readOnly = true)
    public String getMyStatus(Long userId, Long groupId) {
        StudyGroup group = studyGroupRepository.findByGroupIdAndIsDeleted(groupId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디 그룹입니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return studyMemberRepository.findByGroupAndUserAndIsDeleted(group, user, "N")
                .map(StudyMember::getStatus)
                .orElse(null);
    }
}