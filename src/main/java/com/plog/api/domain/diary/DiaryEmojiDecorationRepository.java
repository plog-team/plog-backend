package com.plog.api.domain.diary;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryEmojiDecorationRepository extends JpaRepository<DiaryEmojiDecoration, Long> {
    List<DiaryEmojiDecoration> findAllByDiaryIdAndDeletedFalseOrderByIdAsc(Long diaryId);

    Optional<DiaryEmojiDecoration> findByIdAndDiaryIdAndDeletedFalse(Long id, Long diaryId);
}
