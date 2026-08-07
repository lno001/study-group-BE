package com.study.group.group.repository;

import com.study.group.group.entity.StudyGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    Optional<StudyGroup> findByGroupIdAndIsDeleted(Long groupId, String isDeleted);

    Page<StudyGroup> findByIsDeleted(String isDeleted, Pageable pageable);

    Page<StudyGroup> findByRegionAndIsDeleted(String region, String isDeleted, Pageable pageable);

    Page<StudyGroup> findByStatusAndIsDeleted(String status, String isDeleted, Pageable pageable);

    Page<StudyGroup> findByRegionAndStatusAndIsDeleted(
            String region, String status, String isDeleted, Pageable pageable
    );

    @Query("""
            SELECT DISTINCT g FROM StudyGroup g
            LEFT JOIN StudyMember m ON m.group = g
            WHERE g.isDeleted = 'N'
              AND (
                   g.leader.userId = :userId
                   OR (m.user.userId = :userId AND m.status = '수락' AND m.isDeleted = 'N')
              )
            """)
    Page<StudyGroup> findMyGroups(@Param("userId") Long userId, Pageable pageable);
}