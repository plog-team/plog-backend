package com.plog.api.domain.diary;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plog.api.common.exception.BadRequestException;
import com.plog.api.common.exception.NotFoundException;
import com.plog.api.domain.diary.dto.DiaryResponse;
import com.plog.api.domain.diary.dto.DiaryUpsertRequest;
import com.plog.api.domain.photo.Photo;
import com.plog.api.domain.photo.PhotoRepository;
import com.plog.api.domain.diary.dto.DiarySearchResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final PhotoRepository photoRepository;

    @Transactional
    public DiaryResponse upsert(long userId, DiaryUpsertRequest req) {
        String photoIdsCsv = validateAndJoinPhotoIds(userId, req.photoIds());
        Diary diary = diaryRepository.findByUserIdAndDiaryDate(userId, req.date())
                .orElseGet(() -> Diary.builder()
                        .userId(userId)
                        .diaryDate(req.date())
                        .title(normalizeRequired(req.title(), "title"))
                        .body(normalizeRequired(req.body(), "body"))
                        .location(normalizeOptional(req.location()))
                        .weather(normalizeOptional(req.weather()))
                        .secret(req.secret())
                        .bookmarked(req.bookmarked())
                        .representativePhotoIndex(req.representativePhotoIndex())
                        .photoIdsCsv(photoIdsCsv)
                        .build());

        diary.update(
                req.date(),
                normalizeRequired(req.title(), "title"),
                normalizeRequired(req.body(), "body"),
                normalizeOptional(req.location()),
                normalizeOptional(req.weather()),
                req.secret(),
                req.bookmarked(),
                req.representativePhotoIndex(),
                photoIdsCsv);

        return DiaryResponse.from(diaryRepository.save(diary));
    }

    @Transactional
    public DiaryResponse update(long userId, long diaryId, DiaryUpsertRequest req) {
        String photoIdsCsv = validateAndJoinPhotoIds(userId, req.photoIds());
        Diary diary = diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new NotFoundException("Diary not found id=" + diaryId));
        if (!diary.getDiaryDate().equals(req.date())
                && diaryRepository.existsByUserIdAndDiaryDate(userId, req.date())) {
            throw new BadRequestException("Diary already exists for date=" + req.date());
        }
        diary.update(
                req.date(),
                normalizeRequired(req.title(), "title"),
                normalizeRequired(req.body(), "body"),
                normalizeOptional(req.location()),
                normalizeOptional(req.weather()),
                req.secret(),
                req.bookmarked(),
                req.representativePhotoIndex(),
                photoIdsCsv);
        return DiaryResponse.from(diary);
    }

    @Transactional(readOnly = true)
    public DiaryResponse getByDate(long userId, LocalDate date) {
        return DiaryResponse.from(diaryRepository.findByUserIdAndDiaryDate(userId, date)
                .orElseThrow(() -> new NotFoundException("Diary not found for date=" + date)));
    }

    @Transactional(readOnly = true)
    public DiaryResponse getById(long userId, long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NotFoundException("Diary not found id=" + diaryId));
        if (diary.isSecret() && !diary.getUserId().equals(userId)) {
            throw new BadRequestException("Secret diary is not visible");
        }
        return DiaryResponse.from(diary);
    }

    @Transactional(readOnly = true)
    public List<DiaryResponse> list(long userId, int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), 100));
        return diaryRepository.findAllByUserIdOrderByDiaryDateDesc(userId, pageable).stream()
                .map(DiaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DiaryResponse> listPublic(long userId, int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), 100));
        return diaryRepository.findAllByUserIdAndSecretFalseOrderByDiaryDateDesc(userId, pageable).stream()
                .map(DiaryResponse::from)
                .toList();
    }

    /** 제목/본문/장소/날짜/감정 기준 일기 검색 */
    @Transactional(readOnly = true)
    public List<DiarySearchResponse> search(
            long userId,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            String emotion,
            String sort
    ) {
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedEmotion = normalizeOptional(emotion);

        List<Diary> diaries;

        if ("oldest".equalsIgnoreCase(sort)) {
            diaries = diaryRepository.searchDiariesOldest(
                    userId,
                    normalizedKeyword,
                    startDate,
                    endDate,
                    normalizedEmotion
            );
        } else {
            diaries = diaryRepository.searchDiariesLatest(
                    userId,
                    normalizedKeyword,
                    startDate,
                    endDate,
                    normalizedEmotion
            );
        }

        return diaries.stream()
                .map(diary -> {
                    String primaryEmotion = diaryRepository
                            .findPrimaryEmotion(userId, diary.getId())
                            .orElse(null);

                    return DiarySearchResponse.from(
                            diary,
                            primaryEmotion,
                            representativeImageUrl(userId, diary)
                    );
                })
                .toList();
    }

    /** 대표사진 index 기준으로 이미지 조회 URL 생성 */
    private String representativeImageUrl(long userId, Diary diary) {
        List<Long> photoIds = parsePhotoIds(diary.getPhotoIdsCsv());

        if (photoIds.isEmpty()) {
            return null;
        }

        int safeIndex = Math.max(0, Math.min(diary.getRepresentativePhotoIndex(), photoIds.size() - 1));
        Long representativePhotoId = photoIds.get(safeIndex);

        return "/api/photos/" + representativePhotoId;
    }

    /** photoIdsCsv를 Long 리스트로 변환 */
    private List<Long> parsePhotoIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }

        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .toList();
    }

    @Transactional
    public void delete(long userId, long diaryId) {
        Diary diary = diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new NotFoundException("Diary not found id=" + diaryId));
        diaryRepository.delete(diary);
    }

    private String validateAndJoinPhotoIds(long userId, List<Long> photoIds) {
        if (photoIds == null || photoIds.isEmpty()) {
            return null;
        }
        List<Photo> photos = photoRepository.findAllById(photoIds);
        if (photos.size() != photoIds.size()) {
            throw new NotFoundException("Some photoIds do not exist");
        }
        for (Photo photo : photos) {
            if (!photo.getUserId().equals(userId)) {
                throw new BadRequestException("photoId=" + photo.getId() + " is not owned by current user");
            }
        }
        return photoIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
