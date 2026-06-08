package com.plog.api.domain.aichat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmotionResult {
    private String emotion;      // 기쁨, 슬픔, 불안, 분노, 평온, 혼란, 설렘
    private Float score;         // 0.0 ~ 1.0
    private String summary;      // 한 줄 요약
}