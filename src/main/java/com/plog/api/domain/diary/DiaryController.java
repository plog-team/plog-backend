package com.plog.api.domain.diary;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plog.api.common.UserContext;
import com.plog.api.domain.diary.dto.DiaryResponse;
import com.plog.api.domain.diary.dto.DiaryUpsertRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping
    public DiaryResponse upsert(@Valid @RequestBody DiaryUpsertRequest req) {
        return diaryService.upsert(UserContext.get(), req);
    }

    @PutMapping("/{diaryId}")
    public DiaryResponse update(@PathVariable long diaryId, @Valid @RequestBody DiaryUpsertRequest req) {
        return diaryService.update(UserContext.get(), diaryId, req);
    }

    @GetMapping
    public List<DiaryResponse> list(@RequestParam(defaultValue = "20") int limit) {
        return diaryService.list(UserContext.get(), limit);
    }

    @GetMapping("/by-date/{date}")
    public DiaryResponse getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return diaryService.getByDate(UserContext.get(), date);
    }

    @GetMapping("/{diaryId}")
    public DiaryResponse getById(@PathVariable long diaryId) {
        return diaryService.getById(UserContext.get(), diaryId);
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> delete(@PathVariable long diaryId) {
        diaryService.delete(UserContext.get(), diaryId);
        return ResponseEntity.noContent().build();
    }
}
