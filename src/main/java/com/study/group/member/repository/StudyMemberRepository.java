package com.study.group.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.study.group.group.entity.StudyGroup;
import com.study.group.member.entity.StudyMember;
import com.study.group.user.entity.User;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {

    // 그룹 + 유저로 조회 (중복 신청 체크)
    Optional<StudyMember> findByGroupAndUserAndIsDeleted(
            StudyGroup group, User user, String isDeleted
    );

    // 그룹별 멤버 목록
    List<StudyMember> findByGroupAndIsDeleted(StudyGroup group, String isDeleted);

    // 그룹별 상태 필터 (신청만, 수락만 등)
    List<StudyMember> findByGroupAndStatusAndIsDeleted(
            StudyGroup group, String status, String isDeleted
    );

    // 단건
    Optional<StudyMember> findByMemberIdAndIsDeleted(Long memberId, String isDeleted);
}