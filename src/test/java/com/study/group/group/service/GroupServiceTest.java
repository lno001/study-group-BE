package com.study.group.group.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.study.group.group.dto.GroupCreateRequest;
import com.study.group.group.dto.GroupResponse;
import com.study.group.group.dto.GroupUpdateRequest;
import com.study.group.group.entity.StudyGroup;
import com.study.group.group.repository.StudyGroupRepository;
import com.study.group.member.entity.StudyMember;
import com.study.group.member.repository.StudyMemberRepository;
import com.study.group.user.entity.User;
import com.study.group.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock private StudyGroupRepository studyGroupRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudyMemberRepository studyMemberRepository;

    @InjectMocks
    private GroupService groupService;

    private User leader(Long id) {
        return User.builder()
                .userId(id)
                .loginId("leader")
                .nickname("리더")
                .password("pw")
                .role("USER")
                .isDeleted("N")
                .build();
    }

    private GroupCreateRequest createRequest(String meetingType, String region) {
        GroupCreateRequest req = new GroupCreateRequest();
        req.setTitle("자바 스터디");
        req.setDescription("설명");
        req.setMaxMembers(5);
        req.setMeetingType(meetingType);
        req.setRegion(region);
        req.setSubject("자바");
        req.setAgeRange("무관");
        req.setGender("무관");
        req.setIsPublic("Y");
        return req;
    }

    // ===== 생성 =====

    @Test
    @DisplayName("그룹 생성 성공 - 리더 멤버 수락 저장")
    void createGroup_success() {
        User leader = leader(1L);
        GroupCreateRequest req = createRequest("온라인", null);

        given(userRepository.findById(1L)).willReturn(Optional.of(leader));
        given(studyGroupRepository.save(any(StudyGroup.class))).willAnswer(inv -> {
            StudyGroup g = inv.getArgument(0);
            g.setGroupId(10L);
            return g;
        });
        given(studyMemberRepository.save(any(StudyMember.class)))
                .willAnswer(inv -> inv.getArgument(0));

        GroupResponse res = groupService.createGroup(1L, req);

        assertEquals("자바 스터디", res.getTitle());
        assertEquals(1L, res.getLeaderId());
        verify(studyGroupRepository).save(any(StudyGroup.class));
        verify(studyMemberRepository).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("그룹 생성 실패 - 회원 없음")
    void createGroup_userNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> groupService.createGroup(99L, createRequest("온라인", null)));
        verify(studyGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("그룹 생성 실패 - 탈퇴 회원")
    void createGroup_deletedUser() {
        User deleted = leader(1L);
        deleted.setIsDeleted("Y");
        given(userRepository.findById(1L)).willReturn(Optional.of(deleted));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> groupService.createGroup(1L, createRequest("온라인", null)));
        assertTrue(ex.getMessage().contains("탈퇴"));
    }

    @Test
    @DisplayName("그룹 생성 실패 - 오프라인인데 지역 없음")
    void createGroup_offlineWithoutRegion() {
        given(userRepository.findById(1L)).willReturn(Optional.of(leader(1L)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> groupService.createGroup(1L, createRequest("오프라인", null)));
        assertTrue(ex.getMessage().contains("지역"));
        verify(studyGroupRepository, never()).save(any());
    }

    // ===== 상세 =====

    @Test
    @DisplayName("그룹 상세 성공")
    void getGroup_success() {
        User leader = leader(1L);
        StudyGroup group = StudyGroup.builder()
                .groupId(10L)
                .title("스터디")
                .maxMembers(5)
                .currentMembers(1)
                .status("모집중")
                .meetingType("온라인")
                .leader(leader)
                .isDeleted("N")
                .isPublic("Y")
                .build();

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N"))
                .willReturn(Optional.of(group));

        GroupResponse res = groupService.getGroup(10L);

        assertEquals(10L, res.getGroupId());
        assertEquals("스터디", res.getTitle());
    }

    @Test
    @DisplayName("그룹 상세 실패 - 없음")
    void getGroup_notFound() {
        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N"))
                .willReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> groupService.getGroup(10L));
    }

    // ===== 수정 =====

    @Test
    @DisplayName("그룹 수정 성공")
    void updateGroup_success() {
        User leader = leader(1L);
        StudyGroup group = StudyGroup.builder()
                .groupId(10L)
                .title("이전")
                .maxMembers(5)
                .currentMembers(2)
                .status("모집중")
                .meetingType("온라인")
                .leader(leader)
                .isDeleted("N")
                .isPublic("Y")
                .build();

        GroupUpdateRequest req = new GroupUpdateRequest();
        req.setTitle("수정됨");
        req.setDescription("새 설명");
        req.setMaxMembers(5);
        req.setMeetingType("온라인");
        req.setSubject("자바");
        req.setAgeRange("무관");
        req.setGender("무관");
        req.setStatus("모집중");
        req.setIsPublic("Y");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N"))
                .willReturn(Optional.of(group));

        GroupResponse res = groupService.updateGroup(1L, 10L, req);

        assertEquals("수정됨", res.getTitle());
        assertEquals("수정됨", group.getTitle());
    }

    @Test
    @DisplayName("그룹 수정 실패 - 리더 아님")
    void updateGroup_notLeader() {
        StudyGroup group = StudyGroup.builder()
                .groupId(10L)
                .title("스터디")
                .maxMembers(5)
                .currentMembers(1)
                .meetingType("온라인")
                .leader(leader(1L))
                .isDeleted("N")
                .build();

        GroupUpdateRequest req = new GroupUpdateRequest();
        req.setTitle("해킹");
        req.setMaxMembers(5);
        req.setMeetingType("온라인");
        req.setStatus("모집중");
        req.setIsPublic("Y");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N"))
                .willReturn(Optional.of(group));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> groupService.updateGroup(2L, 10L, req));
        assertTrue(ex.getMessage().contains("권한"));
    }

    @Test
    @DisplayName("그룹 수정 실패 - 정원이 현재 인원보다 작음")
    void updateGroup_maxMembersTooSmall() {
        StudyGroup group = StudyGroup.builder()
                .groupId(10L)
                .title("스터디")
                .maxMembers(5)
                .currentMembers(3)
                .meetingType("온라인")
                .leader(leader(1L))
                .isDeleted("N")
                .build();

        GroupUpdateRequest req = new GroupUpdateRequest();
        req.setTitle("스터디");
        req.setMaxMembers(2); // 현재 3명보다 작음
        req.setMeetingType("온라인");
        req.setStatus("모집중");
        req.setIsPublic("Y");

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N"))
                .willReturn(Optional.of(group));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> groupService.updateGroup(1L, 10L, req));
        assertTrue(ex.getMessage().contains("정원"));
    }

    // ===== 삭제 =====

    @Test
    @DisplayName("그룹 삭제 성공 - 소프트 삭제")
    void deleteGroup_success() {
        StudyGroup group = StudyGroup.builder()
                .groupId(10L)
                .title("스터디")
                .maxMembers(5)
                .currentMembers(1)
                .meetingType("온라인")
                .leader(leader(1L))
                .isDeleted("N")
                .build();

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N"))
                .willReturn(Optional.of(group));

        groupService.deleteGroup(1L, 10L);

        assertEquals("Y", group.getIsDeleted());
    }

    @Test
    @DisplayName("그룹 삭제 실패 - 리더 아님")
    void deleteGroup_notLeader() {
        StudyGroup group = StudyGroup.builder()
                .groupId(10L)
                .title("스터디")
                .maxMembers(5)
                .currentMembers(1)
                .meetingType("온라인")
                .leader(leader(1L))
                .isDeleted("N")
                .build();

        given(studyGroupRepository.findByGroupIdAndIsDeleted(10L, "N"))
                .willReturn(Optional.of(group));

        assertThrows(IllegalArgumentException.class,
                () -> groupService.deleteGroup(2L, 10L));
        assertEquals("N", group.getIsDeleted());
    }

    // ===== 목록 (위임 확인) =====

    @Test
    @DisplayName("그룹 목록 - 필터 없이 조회")
    void getGroups_noFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        User leader = leader(1L);
        StudyGroup group = StudyGroup.builder()
                .groupId(1L)
                .title("A")
                .maxMembers(5)
                .currentMembers(1)
                .status("모집중")
                .meetingType("온라인")
                .leader(leader)
                .isDeleted("N")
                .isPublic("Y")
                .build();

        given(studyGroupRepository.findByIsDeleted("N", pageable))
                .willReturn(new PageImpl<>(List.of(group), pageable, 1));

        var res = groupService.getGroups(null, null, pageable);

        assertEquals(1, res.getContent().size());
        verify(studyGroupRepository).findByIsDeleted("N", pageable);
    }
}