package com.plog.api.pipeline.dto;

import java.util.List;

public record DiaryAnalysisResult(
    List<String> emotions,
    List<String> places,
    boolean needsClarification,
    String question,
    List<String> options
) {}
