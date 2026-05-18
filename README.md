# plog-api (이미지 기반 AI 가이드 백엔드)

> 사진을 입력받아 한국어 일기 초안을 자동 생성하는 Spring Boot 백엔드. 김용진(YongJin04) plog 팀프로젝트 담당 파트.

---

## 빠른 실행

### 사전 요구
- Java 21 LTS
- Docker (MySQL 8.0 컨테이너용)
- Gemini API key (https://aistudio.google.com/app/apikey)

### 1. MySQL 컨테이너 기동
```bash
docker run -d --name plog-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=plog -e MYSQL_DATABASE=plog -e TZ=Asia/Seoul \
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

### 2. `src/main/resources/application-local.yml` 생성 (gitignored)
```yaml
plog:
  gemini:
    api-key: "AIzaSy..."        # 본인 키
    use-mock: false              # true면 Mock 응답
    model: gemini-2.5-flash      # 2026-05 기준 사용 가능 모델
```

### 3. 부팅
```bash
./gradlew bootRun
# Started PlogApiApplication in ~8s
# curl localhost:8080/ping → {"success":true,"data":{"status":"ok","service":"plog-api"}}
```

---

## API 엔드포인트 (10개)

모든 요청에 `X-User-Id: 1` 헤더 필요 (임시 인증).
모든 응답은 `{success, data, error}` 형식.

| 메서드 | 경로 | 용도 |
| --- | --- | --- |
| GET | `/ping` | 헬스체크 |
| POST | `/api/photos` | 멀티파트 사진 1장 업로드 (SHA-256 + 2048px + EXIF 회전 보정) |
| POST | `/api/ai-guide/sessions` | 세션 생성 (vision 분석 + 모드/페르소나) |
| GET | `/api/ai-guide/sessions?limit=N` | 사용자 세션 목록 (대화 검색) |
| GET | `/api/ai-guide/sessions/debug/cache?hash=SHA` | 디버그 cache 조회 |
| GET | `/api/ai-guide/sessions/{id}` | 세션 상세 |
| POST | `/api/ai-guide/sessions/{id}/questions/{qid}/answer` | BATCH 모드 답변 |
| POST | `/api/ai-guide/sessions/{id}/chat` | CONVERSATION 모드 대화 |
| POST | `/api/ai-guide/sessions/{id}/draft` | 한국어 일기 초안 생성 |
| POST | `/api/ai-guide/sessions/{id}/confirm` | 일기 반영 (state_memory 갱신) |
| POST | `/api/ai-guide/sessions/{id}/feedback` | 만족도+코멘트 → 학습 MD 자동 누적 |
| GET/PUT | `/api/users/me/diary-guide` | 사용자 학습 가이드 MD 조회/편집 |

---

## 7-노드 파이프라인

```
[1] ImageImport       PhotoService.upload       SHA-256 + 2048px + EXIF 회전
[2] ExifExtract       ExifExtractNode           metadata-extractor 2.19.0
[3] ContextEnrich     ContextEnrichNode         season/dayOfWeek + GPS (OpenWeather Mock)
[4] VisionAnalysis    VisionAnalysisNode        Gemini 2.5 Flash + SHA-256 캐시
[5] GuideQuestion     GuideQuestionNode         BATCH: 한국어 질문 5개
    | continueConv    GeminiClient              CONVERSATION: 친구 톤 1턴 대화
[6] DraftGenerate     GeminiDraftNode           페르소나 + 학습 MD 반영 한국어 일기
                      DraftGenerateNode         DEFAULT + 가이드없음: 결정론 fallback
[7] Feedback          submitFeedback            user_state_memory 학습 MD 자동 누적
```

진입점: **`AiGuideOrchestrator`** (Controller가 경유, LangGraph 개념 Java state machine 명시화).

---

## 페르소나 5종 + 사용자 학습 MD

| 페르소나 | 톤 |
| --- | --- |
| DEFAULT | 담백·일상체, 시적 표현 금지 |
| FRIENDLY | "~네요/~죠" 다정한 친구 톤 |
| EMOTIONAL | 비유 풍부, 분위기 묘사 |
| FORMAL | "~다" 격식체 보고서 톤 |
| WITTY | 짧고 솔직, 가벼운 농담 |

**사용자 학습 MD**: `user_state_memory(key="diary_guide_md")`. 피드백마다 Gemini가 자동으로 20줄 이내 MD로 재정리·누적. 다음 세션 일기 작성 시 system prompt에 자동 주입.

---

## DB 8 테이블 (Hibernate ddl-auto: update)

```
app_user                          (사용자, seed id=1)
photo                             (업로드 사진, sha256+storedPath+EXIF)
image_analysis_cache              (sha256 unique → Gemini vision JSON)
ai_session                        (mode: BATCH|CONVERSATION, persona enum)
guide_question                    (BATCH 모드 — orderIdx + answer nullable)
chat_message                      (CONVERSATION 모드 — USER/ASSISTANT 교차)
user_feedback                     (sessionId + score + comment)
user_state_memory                 ((userId, key) unique → JSON
                                   key=last_completed_session_id,
                                       confirmed_diary_count,
                                       diary_guide_md)
```

---

## 핵심 설계 결정

| # | 결정 | 이유 |
| --- | --- | --- |
| 1 | Mock 우선 + Real 1줄 토글 | API key 없이도 풀스택 개발/검증 가능 |
| 2 | SHA-256 콘텐츠 캐시 | 동일 사진 재호출 1~5ms (250x 가속), 무료 quota 보존 |
| 3 | 결정론 fallback + Gemini 분기 | DEFAULT + 가이드 없음이면 Gemini 호출 0, 비용 절감 |
| 4 | `@ConditionalOnProperty(use-mock)` | Bean 분기로 코드 변경 없이 모드 전환 |
| 5 | `thinkingConfig: thinkingBudget:0` | Gemini 2.5의 thinking이 응답 truncate 유발, 비활성 |
| 6 | `user_state_memory` 재사용 | 학습 가이드 + 통계를 KV로 통합 관리 |
| 7 | `ApiResponseAdvice` 자동 wrap | 명세 §6 공통 응답 래퍼 자동 적용 |

---

## 핵심 파일 트리

```
src/main/java/com/plog/api/
├── PlogApiApplication.java
├── common/                             ApiResponse, ApiResponseAdvice,
│                                       UserContext, exception/*
├── config/                             JpaConfig, WebMvcConfig,
│                                       UserIdInterceptor, WebClientConfig
├── domain/
│   ├── BaseTimeEntity.java
│   ├── user/                           User + Seeder
│   ├── photo/                          Photo + Service + Controller
│   ├── cache/                          ImageAnalysisCache
│   └── aiguide/                        ★ 본 파트 핵심
│       ├── AiGuideOrchestrator.java   ★ state machine
│       ├── AiSessionService.java      ★ 노드 호출
│       ├── AiSessionController.java
│       ├── DiaryGuideController.java
│       ├── DiaryGuideService.java
│       ├── StateMemoryService.java
│       ├── Persona.java (enum 5종)
│       ├── AiSession (mode + persona)
│       ├── ChatMessage / GuideQuestion / UserFeedback / UserStateMemory
│       └── dto/                       11개
├── pipeline/                          ★ 7-노드
│   ├── ExifExtractNode.java
│   ├── ContextEnrichNode.java
│   ├── VisionAnalysisNode.java
│   ├── GuideQuestionNode.java
│   ├── DraftGenerateNode.java
│   ├── GeminiDraftNode.java
│   └── dto/                           ExifResult, VisionResult,
│                                      ContextResult, ChatTurn, ChatResponse
├── llm/                               ★ LLM 추상화
│   ├── GeminiClient.java (interface)
│   ├── GeminiClientMock.java
│   └── GeminiClientReal.java
└── util/                              Sha256Hasher, ImageResizer
```

---

## 비용 최적화 5종

1. **SHA-256 캐시** — 동일 사진 Gemini 호출 0
2. **Thumbnailator 2048px 리사이즈** — Gemini 토큰 절약
3. **Offline 노드 최대 활용** — EXIF·ContextEnrich 모두 로컬
4. **State Memory 학습 가이드** — 재분석 최소화
5. **Batch 처리** — 여러 사진을 단일 세션으로

---

## 비고

- 임시 인증: `X-User-Id: 1` 헤더 (팀 auth 미정)
- 원격 Git push 금지 (사용자 정책, 2026-05-17)
- API key는 `application-local.yml`(gitignored)에만 저장
