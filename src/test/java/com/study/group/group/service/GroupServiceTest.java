package com.study.group.group.service;

import com.study.group.group.dto.GroupCreateRequest;
import com.study.group.group.dto.GroupResponse;
import com.study.group.group.entity.StudyGroup;
import com.study.group.group.repository.StudyGroupRepository;
import com.study.group.member.repository.StudyMemberRepository;
import com.study.group.user.entity.User;
import com.study.group.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @InjectMocks
    private GroupService groupService;

    @Test
    @DisplayName("그룹 생성 성공")
    void createGroup_success() {
        // given
        Long leaderId = 1L;
        User leader = User.builder()
                .userId(leaderId)
                .loginId("testuser")
                .nickname("테스트")
                .password("encoded")
                .isDeleted("N")
                .build();

        GroupCreateRequest request = new GroupCreateRequest();
        request.setTitle("자바 스터디");
        request.setDescription("설명");
        request.setMaxMembers(5);
        request.setMeetingType("온라인");
        request.setSubject("자바");
        request.setAgeRange("무관");
        request.setGender("무관");
        request.setIsPublic("Y");

        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));
        given(studyGroupRepository.save(any(StudyGroup.class))).willAnswer(inv -> {
            StudyGroup g = inv.getArgument(0);
            g.setGroupId(10L);
            return g;
        });
        given(studyMemberRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        GroupResponse response = groupService.createGroup(leaderId, request);

        // then
        assertThat(response.getTitle()).isEqualTo("자바 스터디");
        assertThat(response.getLeaderId()).isEqualTo(leaderId);
        verify(studyGroupRepository).save(any(StudyGroup.class));
        verify(studyMemberRepository).save(any());
    }

    @Test
    @DisplayName("없는 회원이면 예외")
    void createGroup_userNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        GroupCreateRequest request = new GroupCreateRequest();
        request.setTitle("테스트");
        request.setMaxMembers(5);
        request.setMeetingType("온라인");

        assertThatThrownBy(() -> groupService.createGroup(99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 회원");
    }
}