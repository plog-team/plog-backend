package com.plog.api.pipeline;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.plog.api.domain.aiguide.GuideQuestion;
import com.plog.api.pipeline.dto.VisionResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DraftGenerateNode {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN);

    /** 마무리 문장 후보. emotion·답변 길이·요일 기반 결정론 선택으로 매번 다른 문장 제공. */
    private static final String[] CLOSINGS = {
            "이 정도면 충분한 하루였다.",
            "한 번 더 떠올려도 좋을 시간이다.",
            "내일도 비슷한 하루였으면 좋겠다.",
            "사진을 보면 이날의 한 장면이 다시 떠오른다.",
            "이런 날이 또 와도 반가울 것 같다."
    };

    /**
     * Gemini 초안 생성 실패 시 진입하는 결정론 fallback.
     * vision + 답변을 1인칭 일기체로 구성해 ~250자 일기 생성.
     */
    public String generate(VisionResult vision, List<GuideQuestion> questions) {
        String today = LocalDate.now().format(DATE_FMT);
        String scene = nonBlank(vision == null ? null : vision.scene(), null);
        String emotion = nonBlank(vision == null ? null : vision.suggestedEmotion(), null);
        String mood = nonBlank(vision == null ? null : vision.mood(), null);

        StringBuilder sb = new StringBuilder(384);
        sb.append(today).append("\n\n");

        // 1인칭 도입 — vision.oneLineSummary 직접 인용 금지
        sb.append(buildOpening(scene)).append(" ");

        // 답변 자연 연결 — "돌아보면 / 그리고" 접속어 제거, 일기체 변환
        int answeredCount = 0;
        if (questions != null) {
            for (GuideQuestion q : questions) {
                String ans = q.getAnswer();
                if (ans == null || ans.isBlank()) continue;
                answeredCount++;
                String diaryStyle = toDiaryStyle(ans.trim());
                sb.append(diaryStyle);
                if (needsPeriod(diaryStyle)) sb.append(".");
                sb.append(" ");
            }
        }

        // 답변 없을 때 vision 정보로 본문 보강.
        if (answeredCount == 0) {
            if (mood != null) {
                sb.append("분위기는 ").append(firstWord(mood)).append("이었다. ");
            }
            if (vision != null && vision.oneLineSummary() != null && !vision.oneLineSummary().isBlank()) {
                sb.append("사진 속에는 ").append(vision.oneLineSummary()).append(" ");
            }
        }

        // 마무리 다양화 — emotion이 있으면 inline, 없으면 후보 N개 중 결정론 선택
        sb.append("\n\n").append(buildClosing(emotion, answeredCount));

        String draft = sb.toString().trim();
        log.info("Draft (fallback) generated: {} chars, answered={}", draft.length(), answeredCount);
        return draft;
    }

    /** vision.scene에서 장소를 추출해 1인칭 일기 도입부 구성. */
    private String buildOpening(String scene) {
        if (scene == null || scene.isBlank()) {
            return "오늘은 짧지만 기억에 남는 시간을 보냈다.";
        }
        String place = extractPlaceKeyword(scene);
        if (place != null) {
            return "오늘은 " + place + "에 다녀왔다.";
        }
        return "오늘은 사진에 남은 시간을 보냈다.";
    }

    /** scene 문장에서 "카페", "공원", "식당" 등 장소성 키워드 1개 추출. 없으면 null. */
    private static String extractPlaceKeyword(String scene) {
        String[] places = {"카페", "공원", "바다", "산", "강", "식당", "집", "거리", "골목", "전시", "박물관",
                "공항", "역", "마트", "도서관", "학교", "회사", "사무실", "방", "테라스"};
        for (String p : places) {
            if (scene.contains(p)) return p;
        }
        return null;
    }

    /** 사용자 답변을 1인칭 일기체로 변환. */
    private static String toDiaryStyle(String s) {
        if (s == null || s.isBlank()) return "";
        String t = s.trim();
        // 흔한 반말 종결 → 일기체. 종결부호 제거 후 변환, 다시 부호는 호출부에서 처리.
        String stripped = stripTrailingPunct(t);
        // 2자 종결 패턴 (긴 패턴 우선 매칭)
        if (stripped.endsWith("했잖아")) return stripped.substring(0, stripped.length() - 3) + "했었다";
        if (stripped.endsWith("했어")) return stripped.substring(0, stripped.length() - 2) + "했다";
        if (stripped.endsWith("했지")) return stripped.substring(0, stripped.length() - 2) + "했다";
        if (stripped.endsWith("했거든")) return stripped.substring(0, stripped.length() - 3) + "했다";
        if (stripped.endsWith("잖아")) return stripped.substring(0, stripped.length() - 2) + "었다";
        if (stripped.endsWith("거든")) return stripped.substring(0, stripped.length() - 2) + "었다";
        if (stripped.endsWith("었어")) return stripped.substring(0, stripped.length() - 2) + "었다";
        if (stripped.endsWith("았어")) return stripped.substring(0, stripped.length() - 2) + "았다";
        if (stripped.endsWith("이야")) return stripped.substring(0, stripped.length() - 2) + "이었다";
        if (stripped.endsWith("이지")) return stripped.substring(0, stripped.length() - 2) + "이었다";
        if (stripped.endsWith("더라")) return stripped.substring(0, stripped.length() - 2) + "였다";
        if (stripped.endsWith("같아")) return stripped.substring(0, stripped.length() - 2) + "같았다";
        if (stripped.endsWith("좋아")) return stripped.substring(0, stripped.length() - 2) + "좋았다";
        if (stripped.endsWith("싫어")) return stripped.substring(0, stripped.length() - 2) + "싫었다";
        if (stripped.endsWith("나네")) return stripped.substring(0, stripped.length() - 2) + "났다";
        if (stripped.endsWith("구나")) return stripped.substring(0, stripped.length() - 2) + "다";
        // 1자 종결 (마지막에)
        if (stripped.endsWith("네")) return stripped.substring(0, stripped.length() - 1) + "다";
        if (stripped.endsWith("지")) return stripped.substring(0, stripped.length() - 1) + "다";
        if (stripped.endsWith("야")) return stripped.substring(0, stripped.length() - 1) + "다";
        // 어/까 등 변환 어색한 케이스는 원형 유지
        return stripped;
    }

    private static String stripTrailingPunct(String s) {
        int end = s.length();
        while (end > 0) {
            char c = s.charAt(end - 1);
            if (c == '.' || c == '!' || c == '?' || c == '~' || c == '…') end--;
            else break;
        }
        return s.substring(0, end);
    }

    /** 1인칭 자연 감상으로 마무리 문장 생성. */
    private String buildClosing(String emotion, int answeredCount) {
        if (emotion != null && answeredCount > 0) {
            return "마음에는 " + emotion + "이 남았다.";
        }
        // emotion 없거나 답변 0개 → 후보 5개 중 결정론 선택 (날짜+답변수 hash로 매번 동일 세션에서 동일)
        int idx = Math.floorMod((LocalDate.now().getDayOfYear() * 7) + answeredCount, CLOSINGS.length);
        return CLOSINGS[idx];
    }

    private static String nonBlank(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    /** 콤마/공백으로 구분된 다중 단어에서 첫 단어만 반환. */
    private static String firstWord(String s) {
        if (s == null) return "";
        String t = s.trim();
        int comma = t.indexOf(',');
        if (comma > 0) t = t.substring(0, comma).trim();
        int space = t.indexOf(' ');
        if (space > 0) t = t.substring(0, space).trim();
        return t;
    }

    private static boolean needsPeriod(String s) {
        if (s == null || s.isBlank()) return false;
        String t = s.trim();
        char c = t.charAt(t.length() - 1);
        return c != '.' && c != '!' && c != '?' && c != '~' && c != '…' && c != ')' && c != '」' && c != '"';
    }
}
