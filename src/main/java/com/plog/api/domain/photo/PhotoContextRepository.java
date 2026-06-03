package com.plog.api.domain.photo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoContextRepository extends JpaRepository<PhotoContext, Long> {
    Optional<PhotoContext> findByPhotoId(Long photoId);
}