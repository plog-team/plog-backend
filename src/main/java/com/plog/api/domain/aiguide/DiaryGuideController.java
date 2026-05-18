package com.plog.api.domain.aiguide;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plog.api.common.UserContext;
import com.plog.api.domain.aiguide.dto.DiaryGuideResponse;
import com.plog.api.domain.aiguide.dto.DiaryGuideUpdateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me/diary-guide")
@RequiredArgsConstructor
public class DiaryGuideController {

    private final DiaryGuideService diaryGuideService;

    @GetMapping
    public DiaryGuideResponse get() {
        long userId = UserContext.get();
        String md = diaryGuideService.getGuide(userId).orElse("");
        return DiaryGuideResponse.builder()
                .userId(userId)
                .guideMd(md)
                .lineCount(md.isEmpty() ? 0 : md.split("\\r?\\n").length)
                .maxLines(DiaryGuideService.MAX_LINES)
                .build();
    }

    @PutMapping
    public DiaryGuideResponse put(@Valid @RequestBody DiaryGuideUpdateRequest req) {
        long userId = UserContext.get();
        String saved = diaryGuideService.saveGuide(userId, req.guideMd());
        return DiaryGuideResponse.builder()
                .userId(userId)
                .guideMd(saved)
                .lineCount(saved.isEmpty() ? 0 : saved.split("\\r?\\n").length)
                .maxLines(DiaryGuideService.MAX_LINES)
                .build();
    }
}
