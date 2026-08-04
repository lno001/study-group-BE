package com.study.group.group.entity;

import com.study.group.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "UNI_STUDY_GROUP")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @Column(name = "current_members", nullable = false)
    private Integer currentMembers = 1;  // 개설자 포함

    @Column(nullable = false, length = 20)
    private String status = "모집중";  // 모집중 / 마감

    @Column(name = "meeting_type", nullable = false, length = 20)
    private String meetingType;  // 온라인 / 오프라인 / 혼합

    @Column(length = 30)
    private String region;  // 강남구 등 (온라인일 경우 null 가능)

    @Column(length = 50)
    private String subject;  // 중등수학, 고등국어 등

    @Column(name = "age_range", length = 30)
    private String ageRange;  // 20대, 무관 등

    @Column(length = 10)
    private String gender;  // 남 / 여 / 무관

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", length = 1)
    private String isDeleted = "N";

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.currentMembers == null) {
            this.currentMembers = 1;
        }
        if (this.status == null) {
            this.status = "모집중";
        }
        if (this.isDeleted == null) {
            this.isDeleted = "N";
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}