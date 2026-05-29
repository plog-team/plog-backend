package com.plog.api.domain.diary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Optional<Diary> findByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);

    Optional<Diary> findByIdAndUserId(Long id, Long userId);

    List<Diary> findAllByUserIdOrderByDiaryDateDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);
}
