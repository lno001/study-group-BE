package com.study.group.group.repository;

import com.study.group.group.entity.StudyGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    // 단건 조회 (상세용) — Page 아님
    Optional<StudyGroup> findByGroupIdAndIsDeleted(Long groupId, String isDeleted);

    // 목록 조회 (페이징)
    Page<StudyGroup> findByIsDeleted(String isDeleted, Pageable pageable);

    Page<StudyGroup> findByRegionAndIsDeleted(String region, String isDeleted, Pageable pageable);

    Page<StudyGroup> findByStatusAndIsDeleted(String status, String isDeleted, Pageable pageable);

    Page<StudyGroup> findByRegionAndStatusAndIsDeleted(
            String region, String status, String isDeleted, Pageable pageable
    );
}