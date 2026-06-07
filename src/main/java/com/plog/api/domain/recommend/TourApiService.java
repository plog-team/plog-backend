package com.plog.api.domain.recommend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.domain.recommend.dto.CongestionDto;
import com.plog.api.domain.recommend.dto.PlaceDetailDto;
import com.plog.api.domain.recommend.dto.PlaceItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TourApiService {

    private final WebClient tourClient;
    private final WebClient seoulClient;
    private final String tourApiKey;
    private final String seoulApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public TourApiService(
            @Value("${plog.tour.base-url}") String tourBaseUrl,
            @Value("${plog.tour.api-key}") String tourApiKey,
            @Value("${plog.seoul.base-url}") String seoulBaseUrl,
            @Value("${plog.seoul.api-key}") String seoulApiKey) {
        this.tourClient  = WebClient.builder().baseUrl(tourBaseUrl).build();
        this.seoulClient = WebClient.builder().baseUrl(seoulBaseUrl).build();
        this.tourApiKey  = tourApiKey;
        this.seoulApiKey = seoulApiKey;
    }

    // 주변 장소 조회 → PlaceItemDto 리스트로 변환
    public List<PlaceItemDto> getNearby(double mapX, double mapY, int radius,
                                        int numOfRows, int pageNo, String contentTypeId) {
        String json = tourClient.get()
                .uri(u -> u.path("/locationBasedList2")
                        .queryParam("serviceKey",    tourApiKey)
                        .queryParam("mapX",          mapX)
                        .queryParam("mapY",          mapY)
                        .queryParam("radius",        radius)
                        .queryParam("MobileApp",     "Plog")
                        .queryParam("MobileOS",      "AND")
                        .queryParam("_type",         "json")
                        .queryParam("numOfRows",     numOfRows)
                        .queryParam("pageNo",        pageNo)
                        .queryParam("contentTypeId", contentTypeId)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<PlaceItemDto> result = new ArrayList<>();
        try {
            JsonNode root  = mapper.readTree(json);
            JsonNode items = root.path("response").path("body").path("items").path("item");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    result.add(PlaceItemDto.builder()
                            .contentId(item.path("contentid").asText())
                            .title(item.path("title").asText())
                            .address(item.path("addr1").asText())
                            .imageUrl(item.path("firstimage").asText())
                            .contentTypeId(item.path("contenttypeid").asText())
                            .dist(item.path("dist").asText())
                            .lat(item.path("mapy").asDouble())
                            .lng(item.path("mapx").asDouble())
                            .build());
                }
            } else if (items.isObject()) {
                result.add(PlaceItemDto.builder()
                        .contentId(items.path("contentid").asText())
                        .title(items.path("title").asText())
                        .address(items.path("addr1").asText())
                        .imageUrl(items.path("firstimage").asText())
                        .contentTypeId(items.path("contenttypeid").asText())
                        .dist(items.path("dist").asText())
                        .lat(items.path("mapy").asDouble())
                        .lng(items.path("mapx").asDouble())
                        .build());
            }
        } catch (Exception e) {
            log.error("TourAPI nearby 파싱 오류", e);
        }
        return result;
    }

    // 장소 상세 + 소개 정보 → PlaceDetailDto로 변환
    public PlaceDetailDto getDetail(String contentId, String contentTypeId) {
        PlaceDetailDto.PlaceDetailDtoBuilder builder = PlaceDetailDto.builder();

        // 1단계: detailCommon2 → tel, overview
        try {
            String json = tourClient.get()
                    .uri(u -> u.path("/detailCommon2")
                            .queryParam("serviceKey", tourApiKey)
                            .queryParam("contentId",  contentId)
                            .queryParam("MobileApp",  "Plog")
                            .queryParam("MobileOS",   "AND")
                            .queryParam("_type",      "json")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode item = mapper.readTree(json)
                    .path("response").path("body").path("items").path("item");
            JsonNode d = item.isArray() ? item.get(0) : item;
            builder.tel(d.path("tel").asText())
                    .overview(d.path("overview").asText());
        } catch (Exception e) {
            log.error("TourAPI detail 파싱 오류", e);
        }

        // 2단계: detailIntro2 → usetime, usefee, restdate, 행사일정
        if (contentTypeId != null && !contentTypeId.isEmpty()) {
            try {
                String json = tourClient.get()
                        .uri(u -> u.path("/detailIntro2")
                                .queryParam("serviceKey",    tourApiKey)
                                .queryParam("contentId",     contentId)
                                .queryParam("contentTypeId", contentTypeId)
                                .queryParam("MobileApp",     "Plog")
                                .queryParam("MobileOS",      "AND")
                                .queryParam("_type",         "json")
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode item = mapper.readTree(json)
                        .path("response").path("body").path("items").path("item");
                JsonNode d = item.isArray() ? item.get(0) : item;

                // contentTypeId별 usetime 필드명이 다름
                String usetime = getUsetimeByType(d, contentTypeId);
                String usefee  = getUsefeeByType(d, contentTypeId);
                String restdate = getRestdateByType(d, contentTypeId);

                builder.usetime(usetime)
                        .usefee(usefee)
                        .restdate(restdate)
                        .eventstartdate(d.path("eventstartdate").asText())
                        .eventenddate(d.path("eventenddate").asText());
            } catch (Exception e) {
                log.error("TourAPI intro 파싱 오류", e);
            }
        }

        return builder.build();
    }

    // 서울시 실시간 혼잡도 → CongestionDto로 변환
    public CongestionDto getCongestion(String placeName) {
        try {
            String json = seoulClient.get()
                    .uri("/" + seoulApiKey + "/json/citydata/1/5/" + placeName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = mapper.readTree(json)
                    .path("CITYDATA")
                    .path("LIVE_PPLTN_STTS");
            String level = node.isArray() && node.size() > 0
                    ? node.get(0).path("AREA_CONGEST_LVL").asText()
                    : "정보없음";

            return CongestionDto.builder()
                    .placeName(placeName)
                    .congestionLevel(level)
                    .build();
        } catch (Exception e) {
            log.error("서울시 혼잡도 파싱 오류", e);
            return CongestionDto.builder()
                    .placeName(placeName)
                    .congestionLevel("정보없음")
                    .build();
        }
    }

    // contentTypeId별 usetime 필드명 매핑
    private String getUsetimeByType(JsonNode d, String typeId) {
        switch (typeId) {
            case "12": return d.path("usetimeculture").asText();
            case "14": return d.path("usetimeculture").asText();
            case "28": return d.path("usetimeleports").asText();
            case "32": return d.path("checkintime").asText();
            case "39": return d.path("opentimefood").asText();
            default:   return d.path("usetime").asText();
        }
    }

    private String getUsefeeByType(JsonNode d, String typeId) {
        switch (typeId) {
            case "12": return d.path("usefee").asText();
            case "14": return d.path("usefee").asText();
            case "28": return d.path("usefeeleports").asText();
            case "39": return "";
            default:   return d.path("usefee").asText();
        }
    }

    private String getRestdateByType(JsonNode d, String typeId) {
        switch (typeId) {
            case "14": return d.path("restdateculture").asText();
            case "28": return d.path("restdateleports").asText();
            case "32": return d.path("restdatehotel").asText();
            case "39": return d.path("restdatefood").asText();
            default:   return d.path("restdate").asText();
        }
    }
}