package com.plog.api.domain.photo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PhotoLocationRepository extends JpaRepository<PhotoLocation, Long> {

    /** photoId로 사진 자동입력 정보를 조회 */
    Optional<PhotoLocation> findByPhotoId(Long photoId);

    /** 특정 사용자의 사진 위치 목록 조회 */
    @Query("""
            SELECT pl
            FROM PhotoLocation pl
            JOIN Photo p ON p.id = pl.photoId
            WHERE p.userId = :userId
              AND p.deleted = false
            """)
    List<PhotoLocation> findAllByUserId(@Param("userId") Long userId);
}