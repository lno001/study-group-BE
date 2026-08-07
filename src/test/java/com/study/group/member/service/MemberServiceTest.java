package com.study.group.member.service;

import com.study.group.group.entity.StudyGroup;
import com.study.group.group.repository.StudyGroupRepository;
import com.study.group.member.dto.MemberDecideRequest;
import com.study.group.member.dto.MemberResponse;
import com.study.group.member.entity.StudyMember;
import com.study.group.member.repository.StudyMemberRepository;
import com.study.group.user.entity.User;
import com.study.group.user.repository.UserProfileRepository;
import com.study.group.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private StudyMemberRepository studyMemberRepository;
    @Mock private StudyGroupRepository studyGroupRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;

    @InjectMocks
    private MemberService memberService;

    private User user(Long id, String nickname) {
        return User.builder()
                .userId(id)
                .loginId("user" + id)
                .nickname(nickname)
                .password("pw")
                .role("USER")
                .isDeleted("N")
                .build();
    }

    private StudyGroup group(Long id, User leader, String status, int current, int max, String isPublic) {
        return StudyGroup.builder()
                .groupId(id)
                .title("스터디")
                .maxMembers(max)
                .currentMembers(current)
                .status(status)
                .meetingType("온라인")
                .leader(leader)
                .isDeleted("N")
                .isPublic(isPublic)
                .build();
    }

    // ===== apply =====

    @Test
    @DisplayName("신청 성공")
    void apply_success() {
        User leader = user(1L, "리더");
        User applicant = user(2L, "지원자");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");

        given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(studyMemberRepository.findByGroupAndUserAndIsDeleted(g, applicant, "N"))
                .willReturn(Optional.empty());
        given(studyMemberRepository.save(any(StudyMember.class))).willAnswer(inv -> {
            StudyMember m = inv.getArgument(0);
            m.setMemberId(100L);
            return m;
        });

        MemberResponse res = memberService.apply(2L, 10L);

        assertEquals("신청", res.getStatus());
        assertEquals(2L, res.getUserId());
        verify(studyMemberRepository).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("신청 실패 - 모집 마감")
    void apply_closed() {
        User leader = user(1L, "리더");
        User applicant = user(2L, "지원자");
        StudyGroup g = group(10L, leader, "마감", 5, 5, "Y");

        given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.apply(2L, 10L));
        assertTrue(ex.getMessage().contains("마감"));
        verify(studyMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("신청 실패 - 개설자 본인")
    void apply_leaderCannotApply() {
        User leader = user(1L, "리더");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");

        given(userRepository.findById(1L)).willReturn(Optional.of(leader));
        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.apply(1L, 10L));
        assertTrue(ex.getMessage().contains("개설자"));
    }

    @Test
    @DisplayName("신청 실패 - 정원 초과")
    void apply_full() {
        User leader = user(1L, "리더");
        User applicant = user(2L, "지원자");
        StudyGroup g = group(10L, leader, "모집중", 5, 5, "Y");

        given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));

        assertThrows(IllegalArgumentException.class, () -> memberService.apply(2L, 10L));
    }

    @Test
    @DisplayName("신청 실패 - 이미 신청/참여")
    void apply_alreadyApplied() {
        User leader = user(1L, "리더");
        User applicant = user(2L, "지원자");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");
        StudyMember existing = StudyMember.builder()
                .group(g).user(applicant).status("신청").isDeleted("N").build();

        given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(studyMemberRepository.findByGroupAndUserAndIsDeleted(g, applicant, "N"))
                .willReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.apply(2L, 10L));
        assertTrue(ex.getMessage().contains("이미"));
    }

    // ===== decide =====

    @Test
    @DisplayName("수락 성공 - 인원 증가")
    void decide_accept() {
        User leader = user(1L, "리더");
        User applicant = user(2L, "지원자");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");
        StudyMember member = StudyMember.builder()
                .memberId(100L).group(g).user(applicant).status("신청").isDeleted("N").build();

        MemberDecideRequest req = new MemberDecideRequest();
        req.setStatus("수락");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(studyMemberRepository.findByMemberIdAndIsDeleted(100L, "N"))
                .willReturn(Optional.of(member));
        given(userProfileRepository.findById(2L)).willReturn(Optional.empty());

        MemberResponse res = memberService.decide(1L, 10L, 100L, req);

        assertEquals("수락", res.getStatus());
        assertEquals(2, g.getCurrentMembers());
    }

    @Test
    @DisplayName("수락 시 정원 도달하면 마감")
    void decide_accept_fullThenClose() {
        User leader = user(1L, "리더");
        User applicant = user(2L, "지원자");
        StudyGroup g = group(10L, leader, "모집중", 4, 5, "Y"); // 수락 시 5/5
        StudyMember member = StudyMember.builder()
                .memberId(100L).group(g).user(applicant).status("신청").isDeleted("N").build();

        MemberDecideRequest req = new MemberDecideRequest();
        req.setStatus("수락");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(studyMemberRepository.findByMemberIdAndIsDeleted(100L, "N"))
                .willReturn(Optional.of(member));
        given(userProfileRepository.findById(2L)).willReturn(Optional.empty());

        memberService.decide(1L, 10L, 100L, req);

        assertEquals(5, g.getCurrentMembers());
        assertEquals("마감", g.getStatus());
    }

    @Test
    @DisplayName("거절 성공")
    void decide_reject() {
        User leader = user(1L, "리더");
        User applicant = user(2L, "지원자");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");
        StudyMember member = StudyMember.builder()
                .memberId(100L).group(g).user(applicant).status("신청").isDeleted("N").build();

        MemberDecideRequest req = new MemberDecideRequest();
        req.setStatus("거절");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(studyMemberRepository.findByMemberIdAndIsDeleted(100L, "N"))
                .willReturn(Optional.of(member));
        given(userProfileRepository.findById(2L)).willReturn(Optional.empty());

        MemberResponse res = memberService.decide(1L, 10L, 100L, req);

        assertEquals("거절", res.getStatus());
        assertEquals(1, g.getCurrentMembers()); // 인원 변화 없음
    }

    @Test
    @DisplayName("decide 실패 - 그룹장 아님")
    void decide_notLeader() {
        User leader = user(1L, "리더");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");
        MemberDecideRequest req = new MemberDecideRequest();
        req.setStatus("수락");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.decide(2L, 10L, 100L, req));
        assertTrue(ex.getMessage().contains("권한"));
    }

    @Test
    @DisplayName("decide 실패 - 이미 처리된 신청")
    void decide_alreadyProcessed() {
        User leader = user(1L, "리더");
        User applicant = user(2L, "지원자");
        StudyGroup g = group(10L, leader, "모집중", 2, 5, "Y");
        StudyMember member = StudyMember.builder()
                .memberId(100L).group(g).user(applicant).status("수락").isDeleted("N").build();

        MemberDecideRequest req = new MemberDecideRequest();
        req.setStatus("수락");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(studyMemberRepository.findByMemberIdAndIsDeleted(100L, "N"))
                .willReturn(Optional.of(member));

        assertThrows(IllegalArgumentException.class,
                () -> memberService.decide(1L, 10L, 100L, req));
    }

    // ===== getMembers =====

    @Test
    @DisplayName("멤버 목록 - 공개 그룹 수락 목록")
    void getMembers_publicAccepted() {
        User leader = user(1L, "리더");
        User memberUser = user(2L, "멤버");
        StudyGroup g = group(10L, leader, "모집중", 2, 5, "Y");
        StudyMember m = StudyMember.builder()
                .memberId(100L).group(g).user(memberUser).status("수락").isDeleted("N").build();

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(studyMemberRepository.findByGroupAndStatusAndIsDeleted(g, "수락", "N"))
                .willReturn(List.of(m));

        List<MemberResponse> list = memberService.getMembers(null, 10L, "수락");

        assertEquals(1, list.size());
        assertEquals("멤버", list.get(0).getNickname());
    }

    @Test
    @DisplayName("멤버 목록 실패 - 신청 목록은 그룹장만")
    void getMembers_pendingOnlyLeader() {
        User leader = user(1L, "리더");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(userRepository.findById(2L)).willReturn(Optional.of(user(2L, "다른유저")));
        given(studyMemberRepository.findByGroupAndUserAndIsDeleted(eq(g), any(), eq("N")))
                .willReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.getMembers(2L, 10L, "신청"));
        assertTrue(ex.getMessage().contains("그룹장"));
    }

    @Test
    @DisplayName("멤버 목록 실패 - 비공개 외부인")
    void getMembers_privateBlocked() {
        User leader = user(1L, "리더");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "N"); // 비공개

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.getMembers(null, 10L, "수락"));
        assertTrue(ex.getMessage().contains("비공개"));
    }

    // ===== leave =====

    @Test
    @DisplayName("탈퇴 성공 - 수락 멤버 인원 감소")
    void leave_success() {
        User leader = user(1L, "리더");
        User memberUser = user(2L, "멤버");
        StudyGroup g = group(10L, leader, "모집중", 3, 5, "Y");
        StudyMember m = StudyMember.builder()
                .memberId(100L).group(g).user(memberUser).status("수락").isDeleted("N").build();

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(userRepository.findById(2L)).willReturn(Optional.of(memberUser));
        given(studyMemberRepository.findByGroupAndUserAndIsDeleted(g, memberUser, "N"))
                .willReturn(Optional.of(m));

        memberService.leave(2L, 10L);

        assertEquals("Y", m.getIsDeleted());
        assertEquals(2, g.getCurrentMembers());
    }

    @Test
    @DisplayName("탈퇴 실패 - 개설자")
    void leave_leaderCannot() {
        User leader = user(1L, "리더");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.leave(1L, 10L));
        assertTrue(ex.getMessage().contains("개설자"));
    }

    // ===== kick =====

    @Test
    @DisplayName("강퇴 성공")
    void kick_success() {
        User leader = user(1L, "리더");
        User memberUser = user(2L, "멤버");
        StudyGroup g = group(10L, leader, "모집중", 3, 5, "Y");
        StudyMember m = StudyMember.builder()
                .memberId(100L).group(g).user(memberUser).status("수락").isDeleted("N").build();

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(studyMemberRepository.findByMemberIdAndIsDeleted(100L, "N"))
                .willReturn(Optional.of(m));

        memberService.kick(1L, 10L, 100L);

        assertEquals("Y", m.getIsDeleted());
        assertEquals(2, g.getCurrentMembers());
    }

    @Test
    @DisplayName("강퇴 실패 - 권한 없음")
    void kick_notLeader() {
        User leader = user(1L, "리더");
        StudyGroup g = group(10L, leader, "모집중", 2, 5, "Y");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));

        assertThrows(IllegalArgumentException.class,
                () -> memberService.kick(2L, 10L, 100L));
    }

    @Test
    @DisplayName("강퇴 실패 - 자기 자신")
    void kick_self() {
        User leader = user(1L, "리더");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");
        StudyMember m = StudyMember.builder()
                .memberId(100L).group(g).user(leader).status("수락").isDeleted("N").build();

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(studyMemberRepository.findByMemberIdAndIsDeleted(100L, "N"))
                .willReturn(Optional.of(m));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.kick(1L, 10L, 100L));
        assertTrue(ex.getMessage().contains("자기 자신"));
    }

    // ===== getMyStatus =====

    @Test
    @DisplayName("내 상태 조회 - 신청 중")
    void getMyStatus_applied() {
        User leader = user(1L, "리더");
        User me = user(2L, "나");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");
        StudyMember m = StudyMember.builder()
                .group(g).user(me).status("신청").isDeleted("N").build();

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(userRepository.findById(2L)).willReturn(Optional.of(me));
        given(studyMemberRepository.findByGroupAndUserAndIsDeleted(g, me, "N"))
                .willReturn(Optional.of(m));

        assertEquals("신청", memberService.getMyStatus(2L, 10L));
    }

    @Test
    @DisplayName("내 상태 조회 - 참여 기록 없음 null")
    void getMyStatus_none() {
        User leader = user(1L, "리더");
        User me = user(2L, "나");
        StudyGroup g = group(10L, leader, "모집중", 1, 5, "Y");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N")).willReturn(Optional.of(g));
        given(userRepository.findById(2L)).willReturn(Optional.of(me));
        given(studyMemberRepository.findByGroupAndUserAndIsDeleted(g, me, "N"))
                .willReturn(Optional.empty());

        assertNull(memberService.getMyStatus(2L, 10L));
    }
}