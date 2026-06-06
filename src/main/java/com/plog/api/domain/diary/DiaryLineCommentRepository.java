package com.plog.api.domain.diary;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryLineCommentRepository extends JpaRepository<DiaryLineComment, Long> {
    List<DiaryLineComment> findAllByDiaryIdAndDeletedFalseOrderByLineIndexAscIdAsc(Long diaryId);

    List<DiaryLineComment> findAllByDiaryIdAndLineIndexAndDeletedFalseOrderByIdAsc(Long diaryId, Integer lineIndex);

    Optional<DiaryLineComment> findByIdAndDiaryIdAndDeletedFalse(Long id, Long diaryId);
}
