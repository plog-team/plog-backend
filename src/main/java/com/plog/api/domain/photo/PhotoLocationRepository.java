package com.plog.api.domain.photo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface PhotoLocationRepository extends JpaRepository<PhotoLocation, Long> {
    /** photoId로 사진 자동입력 정보를 조회 */
    Optional<PhotoLocation> findByPhotoId(Long photoId);
}