package com.plog.api.domain.report.dto.response;

import java.util.List;

public record InterruptPayload(
    Long diaryId,
    String date,
    String question,
    List<String> options
) {}
