# Postman API Examples

Base URL:

```text
http://localhost:8080
```

Common header:

```text
X-User-Id: 1
```

## 1. Health Check

```http
GET /ping
```

## 2. Diary API

### Create or update today's diary

```http
POST /api/diaries
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "date": "2026-05-28",
  "title": "가천대에서 보낸 하루",
  "body": "오늘은 팀 프로젝트 회의를 하고 백엔드 API를 Postman으로 확인했다.",
  "location": "가천대학교",
  "weather": "맑음",
  "secret": false,
  "bookmarked": true,
  "representativePhotoIndex": 0,
  "photoIds": [1, 2]
}
```

### List diaries

```http
GET /api/diaries?limit=20
X-User-Id: 1
```

### Get diary by date

```http
GET /api/diaries/by-date/2026-05-28
X-User-Id: 1
```

### Get diary by id

```http
GET /api/diaries/1
X-User-Id: 1
```

### Update diary by id

```http
PUT /api/diaries/1
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "date": "2026-05-28",
  "title": "수정한 일기 제목",
  "body": "수정된 본문입니다.",
  "location": "가천대학교 AI관",
  "weather": "흐림",
  "secret": true,
  "bookmarked": false,
  "representativePhotoIndex": 1,
  "photoIds": [1, 2]
}
```

### Delete diary

```http
DELETE /api/diaries/1
X-User-Id: 1
```

## 2-1. Diary Line Comment API

### List all comments in a diary

```http
GET /api/diaries/1/comments
X-User-Id: 1
```

### List comments by line

```http
GET /api/diaries/1/comments?lineIndex=0
X-User-Id: 1
```

### Create comment

```http
POST /api/diaries/1/comments
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "lineIndex": 0,
  "content": "이 줄에 댓글을 남겨요."
}
```

### Update comment

```http
PUT /api/diaries/1/comments/{commentId}
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "content": "수정한 댓글입니다."
}
```

### Delete comment

```http
DELETE /api/diaries/1/comments/{commentId}
X-User-Id: 1
```

## 2-2. Diary Emoji Decoration API

### List emoji decorations

```http
GET /api/diaries/1/decorations
X-User-Id: 1
```

### Create emoji decoration

```http
POST /api/diaries/1/decorations
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "emoji": "⭐",
  "xRatio": 0.45,
  "yRatio": 0.25,
  "scale": 1.0,
  "rotation": 0.0
}
```

### Update emoji decoration

```http
PUT /api/diaries/1/decorations/{decorationId}
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "emoji": "😊",
  "xRatio": 0.7,
  "yRatio": 0.55,
  "scale": 1.2,
  "rotation": 12.0
}
```

### Delete emoji decoration

```http
DELETE /api/diaries/1/decorations/{decorationId}
X-User-Id: 1
```

## 3. Photo Upload API

Postman Body:

- `form-data`
- key: `files`
- type: `File`
- select 1 to 10 image files

```http
POST /api/photos
X-User-Id: 1
```

The response contains `photoId`; use those IDs in diary and AI guide requests.

## 4. AI Guide API

For these endpoints, upload real images through `/api/photos` first. Dummy SQL photo rows are enough for diary testing, but AI guide reads actual files from `stored_path`.

### Create batch session

```http
POST /api/ai-guide/sessions
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "photoIds": [1],
  "mode": "BATCH",
  "persona": "FRIENDLY"
}
```

### Answer a guide question

```http
POST /api/ai-guide/sessions/{sessionId}/questions/{questionId}/answer
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "answer": "친구들과 프로젝트 이야기를 하면서 꽤 뿌듯했다."
}
```

### Generate draft

```http
POST /api/ai-guide/sessions/{sessionId}/draft
X-User-Id: 1
```

### Confirm draft

```http
POST /api/ai-guide/sessions/{sessionId}/confirm
X-User-Id: 1
```

### Submit feedback

```http
POST /api/ai-guide/sessions/{sessionId}/feedback
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "satisfactionScore": 5,
  "comment": "문장이 자연스럽고 내 말투와 잘 맞았어요."
}
```

## 5. Diary Guide API

```http
GET /api/users/me/diary-guide
X-User-Id: 1
```

```http
PUT /api/users/me/diary-guide
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "guideMd": "일기는 너무 과장하지 말고, 장소와 감정을 구체적으로 써줘."
}
```
