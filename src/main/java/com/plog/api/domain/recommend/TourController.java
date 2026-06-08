package com.plog.api.domain.recommend;

import com.plog.api.domain.recommend.dto.CongestionDto;
import com.plog.api.domain.recommend.dto.PlaceDetailDto;
import com.plog.api.domain.recommend.dto.PlaceItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tour")
@RequiredArgsConstructor
public class TourController {

    private final TourApiService tourApiService;

    // 주변 장소 조회
    @GetMapping("/nearby")
    public List<PlaceItemDto> getNearby(
            @RequestParam double mapX,
            @RequestParam double mapY,
            @RequestParam(defaultValue = "2000") int radius,
            @RequestParam(defaultValue = "20")   int numOfRows,
            @RequestParam(defaultValue = "1")    int pageNo,
            @RequestParam(defaultValue = "")     String contentTypeId) {
        return tourApiService.getNearby(mapX, mapY, radius, numOfRows, pageNo, contentTypeId);
    }

    // 장소 상세 + 소개 정보
    @GetMapping("/detail")
    public PlaceDetailDto getDetail(
            @RequestParam String contentId,
            @RequestParam(defaultValue = "") String contentTypeId) {
        return tourApiService.getDetail(contentId, contentTypeId);
    }

    // 서울시 실시간 혼잡도
    @GetMapping("/congestion/{placeName}")
    public CongestionDto getCongestion(@PathVariable String placeName) {
        return tourApiService.getCongestion(placeName);
    }
}