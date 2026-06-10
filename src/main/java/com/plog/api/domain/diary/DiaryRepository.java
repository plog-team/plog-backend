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

    List<Diary> findAllByUserIdAndSecretFalseOrderByDiaryDateDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);
    List<Diary> findByUserIdAndDiaryDateBetweenOrderByDiaryDateAsc(Long userId, LocalDate start, LocalDate end);

    /** 제목/본문/장소/날짜/감정 기준 일기 검색 - 최신순 */
    @Query(value = """
        SELECT d.*
        FROM diary d
        WHERE d.user_id = :userId
          AND (
            :keyword IS NULL
            OR :keyword = ''
            OR d.title LIKE CONCAT('%', :keyword, '%')
            OR d.body LIKE CONCAT('%', :keyword, '%')
            OR d.location LIKE CONCAT('%', :keyword, '%')
          )
          AND (
            :startDate IS NULL
            OR d.diary_date >= :startDate
          )
          AND (
            :endDate IS NULL
            OR d.diary_date <= :endDate
          )
          AND (
            :emotion IS NULL
            OR :emotion = ''
            OR EXISTS (
                SELECT 1
                FROM emotion_analysis ea
                WHERE ea.diary_id = d.id
                  AND ea.user_id = d.user_id
                  AND ea.primary_emotion = :emotion
            )
            OR EXISTS (
                SELECT 1
                FROM emotion_detail ed
                JOIN emotion_analysis ea2 ON ed.analysis_id = ea2.id
                WHERE ea2.diary_id = d.id
                  AND ea2.user_id = d.user_id
                  AND ed.emotion = :emotion
            )
          )
        ORDER BY d.diary_date DESC
        """, nativeQuery = true)
    List<Diary> searchDiariesLatest(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("emotion") String emotion
    );

    /** 제목/본문/장소/날짜/감정 기준 일기 검색 - 오래된순 */
    @Query(value = """
        SELECT d.*
        FROM diary d
        WHERE d.user_id = :userId
          AND (
            :keyword IS NULL
            OR :keyword = ''
            OR d.title LIKE CONCAT('%', :keyword, '%')
            OR d.body LIKE CONCAT('%', :keyword, '%')
            OR d.location LIKE CONCAT('%', :keyword, '%')
          )
          AND (
            :startDate IS NULL
            OR d.diary_date >= :startDate
          )
          AND (
            :endDate IS NULL
            OR d.diary_date <= :endDate
          )
          AND (
            :emotion IS NULL
            OR :emotion = ''
            OR EXISTS (
                SELECT 1
                FROM emotion_analysis ea
                WHERE ea.diary_id = d.id
                  AND ea.user_id = d.user_id
                  AND ea.primary_emotion = :emotion
            )
            OR EXISTS (
                SELECT 1
                FROM emotion_detail ed
                JOIN emotion_analysis ea2 ON ed.analysis_id = ea2.id
                WHERE ea2.diary_id = d.id
                  AND ea2.user_id = d.user_id
                  AND ed.emotion = :emotion
            )
          )
        ORDER BY d.diary_date ASC
        """, nativeQuery = true)
    List<Diary> searchDiariesOldest(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("emotion") String emotion
    );
    /** diaryId 기준 대표 감정 조회 */
    @Query(value = """
        SELECT ea.primary_emotion
        FROM emotion_analysis ea
        WHERE ea.diary_id = :diaryId
          AND ea.user_id = :userId
        ORDER BY ea.analyzed_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<String> findPrimaryEmotion(
            @Param("userId") Long userId,
            @Param("diaryId") Long diaryId
    );
}
