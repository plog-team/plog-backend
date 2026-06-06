package com.plog.api.domain.report.dto.response;

public record ReportStatusResponse(
    String status,
    String threadId,
    String userName,
    PlaceReportData placeReport,
    EmotionReportData emotionReport,
    InterruptPayload interruptPayload,
    String message
) {}
