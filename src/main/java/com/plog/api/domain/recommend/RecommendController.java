package com.plog.api.domain.recommend;

import com.plog.api.common.UserContext;
import com.plog.api.domain.recommend.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    // 북마크

    // 북마크 추가
    @PostMapping("/bookmarks")
    public ResponseEntity<Map<String, String>> addBookmark(
            @Valid @RequestBody BookmarkRequest req) {
        recommendService.addBookmark(UserContext.get(), req);
        return ResponseEntity.ok(Map.of("message", "북마크가 저장되었습니다"));
    }

    // 내 북마크 목록
    @GetMapping("/bookmarks")
    public List<BookmarkResponse> getBookmarks() {
        return recommendService.getBookmarks(UserContext.get());
    }

    // 북마크 여부 확인
    @GetMapping("/bookmarks/{contentId}/status")
    public Map<String, Boolean> isBookmarked(@PathVariable String contentId) {
        return Map.of("bookmarked",
                recommendService.isBookmarked(UserContext.get(), contentId));
    }

    // 북마크 삭제
    @DeleteMapping("/bookmarks/{contentId}")
    public ResponseEntity<Map<String, String>> removeBookmark(
            @PathVariable String contentId) {
        recommendService.removeBookmark(UserContext.get(), contentId);
        return ResponseEntity.ok(Map.of("message", "북마크가 삭제되었습니다"));
    }

    // 클릭 로그

    // 장소 클릭 기록
    @PostMapping("/clicklog")
    public ResponseEntity<Map<String, String>> saveClickLog(
            @Valid @RequestBody ClickLogRequest req) {
        recommendService.saveClickLog(UserContext.get(), req);
        return ResponseEntity.ok(Map.of("message", "클릭 로그가 저장되었습니다"));
    }

    // 선호 카테고리

    // 선호 카테고리 조회
    @GetMapping("/preference")
    public PreferenceResponse getPreference() {
        return recommendService.getPreference(UserContext.get());
    }

    // 선호 카테고리 업데이트
    @PutMapping("/preference")
    public ResponseEntity<Map<String, String>> updatePreference(
            @RequestBody PreferenceRequest req) {
        recommendService.updatePreference(UserContext.get(), req);
        return ResponseEntity.ok(Map.of("message", "선호 카테고리가 업데이트되었습니다"));
    }

    // 위치 로그

    // 위치 저장 (500m 쓰로틀링용)
    @PostMapping("/location")
    public ResponseEntity<Map<String, String>> saveLocation(
            @Valid @RequestBody LocationRequest req) {
        recommendService.saveLocation(UserContext.get(), req);
        return ResponseEntity.ok(Map.of("message", "위치가 저장되었습니다"));
    }

    // 마지막 위치 조회
    @GetMapping("/location")
    public LocationResponse getLastLocation() {
        return recommendService.getLastLocation(UserContext.get());
    }
}
