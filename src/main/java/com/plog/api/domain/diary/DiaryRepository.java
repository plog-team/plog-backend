package com.plog.api.domain.diary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Optional<Diary> findByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);

    Optional<Diary> findByIdAndUserId(Long id, Long userId);

    List<Diary> findAllByUserIdOrderByDiaryDateDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);
    List<Diary> findByUserIdAndDiaryDateBetweenOrderByDiaryDateAsc(Long userId, LocalDate start, LocalDate end);

    /** 제목/본문/장소 기준 일기 검색 */
    /** 제목/본문/장소 기준 일기 검색 - 최신순 */
    @Query("""
    SELECT d
    FROM Diary d
    WHERE d.userId = :userId
      AND (
        :keyword IS NULL
        OR :keyword = ''
        OR d.title LIKE CONCAT('%', :keyword, '%')
        OR d.body LIKE CONCAT('%', :keyword, '%')
        OR d.location LIKE CONCAT('%', :keyword, '%')
      )
    ORDER BY d.diaryDate DESC
""")
    List<Diary> searchDiariesLatest(
            @Param("userId") Long userId,
            @Param("keyword") String keyword
    );

    /** 제목/본문/장소 기준 일기 검색 - 오래된순 */
    @Query("""
    SELECT d
    FROM Diary d
    WHERE d.userId = :userId
      AND (
        :keyword IS NULL
        OR :keyword = ''
        OR d.title LIKE CONCAT('%', :keyword, '%')
        OR d.body LIKE CONCAT('%', :keyword, '%')
        OR d.location LIKE CONCAT('%', :keyword, '%')
      )
    ORDER BY d.diaryDate ASC
""")
    List<Diary> searchDiariesOldest(
            @Param("userId") Long userId,
            @Param("keyword") String keyword
    );
}
