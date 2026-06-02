package com.plog.api.domain.recommend;

import com.plog.api.common.exception.BadRequestException;
import com.plog.api.domain.recommend.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendService {

    private final BookmarkRepository        bookmarkRepo;
    private final ClickLogRepository        clickLogRepo;
    private final UserPreferenceRepository  preferenceRepo;
    private final UserLocationLogRepository locationRepo;

    // 북마크

    @Transactional
    public void addBookmark(Long userId, BookmarkRequest req) {
        if (bookmarkRepo.existsByUserIdAndContentId(userId, req.contentId())) {
            throw new BadRequestException("이미 북마크된 장소입니다");
        }
        bookmarkRepo.save(Bookmark.builder()
                .userId(userId)
                .contentId(req.contentId())
                .title(req.title())
                .address(req.address())
                .imageUrl(req.imageUrl())
                .category(req.category())
                .contentTypeId(req.contentTypeId())
                .build());
    }

    public List<BookmarkResponse> getBookmarks(Long userId) {
        return bookmarkRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(BookmarkResponse::from).toList();
    }

    @Transactional
    public void removeBookmark(Long userId, String contentId) {
        if (!bookmarkRepo.existsByUserIdAndContentId(userId, contentId)) {
            throw new BadRequestException("북마크가 존재하지 않습니다");
        }
        bookmarkRepo.deleteByUserIdAndContentId(userId, contentId);
    }

    public boolean isBookmarked(Long userId, String contentId) {
        return bookmarkRepo.existsByUserIdAndContentId(userId, contentId);
    }

    // 클릭 로그

    @Transactional
    public void saveClickLog(Long userId, ClickLogRequest req) {
        clickLogRepo.save(ClickLog.builder()
                .userId(userId)
                .contentId(req.contentId())
                .contentTypeId(req.contentTypeId())
                .category(req.category())
                .build());
    }

    // 선호 카테고리

    public PreferenceResponse getPreference(Long userId) {
        return preferenceRepo.findByUserId(userId)
                .map(PreferenceResponse::from)
                .orElse(new PreferenceResponse(userId, List.of(), null));
    }

    @Transactional
    public void updatePreference(Long userId, PreferenceRequest req) {
        UserPreference pref = preferenceRepo.findByUserId(userId)
                .orElse(UserPreference.builder()
                        .userId(userId)
                        .preferredCategories("")
                        .build());
        pref.update(req.preferredCategories());
        preferenceRepo.save(pref);
    }

    // 위치 로그

    @Transactional
    public void saveLocation(Long userId, LocationRequest req) {
        locationRepo.save(UserLocationLog.builder()
                .userId(userId)
                .latitude(req.latitude())
                .longitude(req.longitude())
                .source(req.source() != null ? req.source() : "gps")
                .build());
    }

    public LocationResponse getLastLocation(Long userId) {
        return locationRepo.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(LocationResponse::from)
                .orElse(null);
    }
}
