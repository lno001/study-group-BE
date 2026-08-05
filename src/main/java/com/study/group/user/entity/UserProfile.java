package com.study.group.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "UNI_USER_PROFILE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 10)
    private String gender;          // 남 / 여

    private Integer age;

    @Column(length = 30)
    private String education;       // 중학생, 고등학생, 대학생, 직장인 등

    @Column(length = 30)
    private String region;          // 강남구, 마포구 등

    @Builder.Default
    @Column(name = "gender_public", length = 1)
    private String genderPublic = "N";

    @Builder.Default
    @Column(name = "age_public", length = 1)
    private String agePublic = "N";

    @Builder.Default
    @Column(name = "education_public", length = 1)
    private String educationPublic = "N";

    @Builder.Default
    @Column(name = "region_public", length = 1)
    private String regionPublic = "Y";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}