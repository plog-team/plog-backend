package com.plog.api.domain.report.controller;

import com.plog.api.common.UserContext;
import com.plog.api.domain.report.dto.request.ClarifyRequest;
import com.plog.api.domain.report.dto.request.ReportFeedbackRequest;
import com.plog.api.domain.report.dto.response.GenerateReportResponse;
import com.plog.api.domain.report.dto.response.ReportStatusResponse;
import com.plog.api.domain.report.entity.ReportType;
import com.plog.api.domain.report.service.ReportSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportSessionService reportSessionService;

    @PostMapping("/emotion/generate")
    public GenerateReportResponse generateEmotion() {
        return reportSessionService.generate(UserContext.get(), ReportType.EMOTION);
    }

    @PostMapping("/place/generate")
    public GenerateReportResponse generatePlace() {
        return reportSessionService.generate(UserContext.get(), ReportType.PLACE);
    }

    @GetMapping("/emotion/{threadId}")
    public ReportStatusResponse getEmotionStatus(@PathVariable String threadId) {
        return reportSessionService.getStatus(threadId, UserContext.get());
    }

    @GetMapping("/place/{threadId}")
    public ReportStatusResponse getPlaceStatus(@PathVariable String threadId) {
        return reportSessionService.getStatus(threadId, UserContext.get());
    }

    @PostMapping("/emotion/{threadId}/clarify")
    public void clarifyEmotion(@PathVariable String threadId, @Valid @RequestBody ClarifyRequest req) {
        reportSessionService.clarify(threadId, UserContext.get(), req);
    }

    @PostMapping("/place/{threadId}/clarify")
    public void clarifyPlace(@PathVariable String threadId, @Valid @RequestBody ClarifyRequest req) {
        reportSessionService.clarify(threadId, UserContext.get(), req);
    }

    @PostMapping("/emotion/{threadId}/feedback")
    public void feedbackEmotion(@PathVariable String threadId, @RequestBody ReportFeedbackRequest req) {
        reportSessionService.submitFeedback(threadId, UserContext.get(), req);
    }

    @PostMapping("/place/{threadId}/feedback")
    public void feedbackPlace(@PathVariable String threadId, @RequestBody ReportFeedbackRequest req) {
        reportSessionService.submitFeedback(threadId, UserContext.get(), req);
    }
}
