package com.plog.api.domain.report.dto.response;

import java.util.List;

public record PlaceReportData(
    String period,
    String content,
    String topPhotoUrl,
    List<PlaceEntry> places
) {}
