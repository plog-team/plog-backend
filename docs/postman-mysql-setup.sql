-- plog-backend Postman test setup
-- MySQL 8.0 기준. 현재 Spring Boot 엔티티와 API에 맞춘 최소 스키마 + 더미 데이터입니다.
--
-- 팀 설계안과 현재 구현의 차이:
-- - User 테이블은 MySQL 예약어 충돌을 피하려고 app_user를 사용합니다.
-- - Diary.content는 현재 백엔드에서 diary.body 컬럼입니다.
-- - Diary.visibility, address, manual_latitude, representative_photo_id, is_deleted는 아직 API/엔티티 미구현입니다.
-- - DiaryPhoto는 아직 별도 테이블이 아니고 diary.photo_ids_csv로 임시 저장합니다.
-- - Photo.image_url/file_size는 현재 photo.stored_path/size_bytes 컬럼입니다.

CREATE DATABASE IF NOT EXISTS plog
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE plog;

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  email VARCHAR(100),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS photo (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  mime_type VARCHAR(50) NOT NULL,
  width INT NOT NULL,
  height INT NOT NULL,
  size_bytes BIGINT NOT NULL,
  stored_path VARCHAR(500) NOT NULL,
  captured_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_photo_user (user_id),
  INDEX idx_photo_sha256 (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS diary (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  diary_date DATE NOT NULL,
  title VARCHAR(120) NOT NULL,
  body LONGTEXT NOT NULL,
  location VARCHAR(120) NULL,
  weather VARCHAR(80) NULL,
  secret BIT(1) NOT NULL,
  bookmarked BIT(1) NOT NULL,
  representative_photo_index INT NOT NULL,
  photo_ids_csv VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_diary_user_date (user_id, diary_date),
  INDEX idx_diary_user_date (user_id, diary_date),
  INDEX idx_diary_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS diary_line_comment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  diary_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  line_index INT NOT NULL,
  content LONGTEXT NOT NULL,
  is_deleted BIT(1) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_diary_line_comment_diary_line (diary_id, line_index),
  INDEX idx_diary_line_comment_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS diary_emoji_decoration (
  id BIGINT NOT NULL AUTO_INCREMENT,
  diary_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  emoji VARCHAR(20) NOT NULL,
  x_ratio DOUBLE NOT NULL,
  y_ratio DOUBLE NOT NULL,
  scale DOUBLE NOT NULL,
  rotation DOUBLE NOT NULL,
  is_deleted BIT(1) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_diary_emoji_decoration_diary (diary_id),
  INDEX idx_diary_emoji_decoration_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS image_analysis_cache (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sha256 VARCHAR(64) NOT NULL,
  vision_json LONGTEXT NOT NULL,
  model_version VARCHAR(50) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_iac_sha256 (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  mode VARCHAR(20) NOT NULL,
  persona VARCHAR(20) NOT NULL,
  photo_ids_csv VARCHAR(500) NULL,
  draft LONGTEXT NULL,
  completed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_ai_session_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS guide_question (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  order_idx INT NOT NULL,
  question VARCHAR(500) NOT NULL,
  question_type VARCHAR(16) NULL,
  suggested_answers_json LONGTEXT NULL,
  answer LONGTEXT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_guide_question_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  content LONGTEXT NOT NULL,
  order_idx INT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_chat_message_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_feedback (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  satisfaction_score INT NULL,
  comment LONGTEXT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_user_feedback_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_state_memory (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  memory_key VARCHAR(100) NOT NULL,
  value_json LONGTEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_state_memory_user_key (user_id, memory_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE photo_location (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    photo_id BIGINT NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    taken_at DATETIME(6),
    location_name VARCHAR(255),
    weather VARCHAR(100),
    temperature DOUBLE
);

INSERT INTO app_user (id, name, email, created_at, updated_at)
VALUES
  (1, 'Plog Tester', 'tester@plog.local', NOW(6), NOW(6)),
  (2, 'Exchange Friend', 'friend@plog.local', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  email = VALUES(email),
  updated_at = NOW(6);

-- 이 photo 더미 행은 일기 API의 photoIds 검증용입니다.
-- AI 가이드 API는 실제 이미지 파일을 읽기 때문에 /api/photos로 진짜 이미지를 업로드한 photoId를 쓰는 편이 안전합니다.
INSERT INTO photo (
  id, user_id, sha256, original_filename, mime_type, width, height, size_bytes,
  stored_path, captured_at, created_at, updated_at
)
VALUES
  (
    1,
    1,
    '1111111111111111111111111111111111111111111111111111111111111111',
    'dummy-campus.jpg',
    'image/jpeg',
    1200,
    900,
    245760,
    './uploads/1/dummy-campus.jpg',
    '2026-05-28 14:20:00.000000',
    NOW(6),
    NOW(6)
  ),
  (
    2,
    1,
    '2222222222222222222222222222222222222222222222222222222222222222',
    'dummy-cafe.jpg',
    'image/jpeg',
    1080,
    1080,
    198000,
    './uploads/1/dummy-cafe.jpg',
    '2026-05-28 16:40:00.000000',
    NOW(6),
    NOW(6)
  )
ON DUPLICATE KEY UPDATE
  updated_at = NOW(6);

INSERT INTO diary (
  id, user_id, diary_date, title, body, location, weather, secret, bookmarked,
  representative_photo_index, photo_ids_csv, created_at, updated_at
)
VALUES
  (
    1,
    1,
    '2026-05-28',
    '가천대에서 보낸 하루',
    '오늘은 팀 프로젝트 회의를 하고 백엔드 API를 Postman으로 확인했다. 사진을 고르고 일기로 정리하니 하루가 더 선명하게 남았다.',
    '가천대학교',
    '맑음',
    b'0',
    b'1',
    0,
    '1,2',
    NOW(6),
    NOW(6)
  )
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  body = VALUES(body),
  location = VALUES(location),
  weather = VALUES(weather),
  secret = VALUES(secret),
  bookmarked = VALUES(bookmarked),
  representative_photo_index = VALUES(representative_photo_index),
  photo_ids_csv = VALUES(photo_ids_csv),
  updated_at = NOW(6);

INSERT INTO user_state_memory (user_id, memory_key, value_json, created_at, updated_at)
VALUES
  (1, 'confirmed_diary_count', '1', NOW(6), NOW(6)),
  (1, 'diary_guide_md', '"사용자는 담백하고 구체적인 일기 문체를 선호한다."', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
  value_json = VALUES(value_json),
  updated_at = NOW(6);

INSERT INTO diary_line_comment (
  id, diary_id, user_id, line_index, content, is_deleted, created_at, updated_at
)
VALUES
  (1, 1, 1, 0, '첫 줄 분위기가 좋아요.', b'0', NOW(6), NOW(6)),
  (2, 1, 1, 1, '이 부분은 조금 더 자세히 써도 좋겠어요.', b'0', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  is_deleted = VALUES(is_deleted),
  updated_at = NOW(6);

INSERT INTO diary_emoji_decoration (
  id, diary_id, user_id, emoji, x_ratio, y_ratio, scale, rotation, is_deleted, created_at, updated_at
)
VALUES
  (1, 1, 1, '⭐', 0.45, 0.25, 1.0, 0.0, b'0', NOW(6), NOW(6)),
  (2, 1, 1, '😊', 0.70, 0.55, 1.2, 12.0, b'0', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
  emoji = VALUES(emoji),
  x_ratio = VALUES(x_ratio),
  y_ratio = VALUES(y_ratio),
  scale = VALUES(scale),
  rotation = VALUES(rotation),
  is_deleted = VALUES(is_deleted),
  updated_at = NOW(6);
