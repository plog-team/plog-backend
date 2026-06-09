package com.plog.api.exchange.controller;

import com.plog.api.common.UserContext;
import com.plog.api.exchange.dto.PreferenceUpdateRequest;
import com.plog.api.exchange.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchange/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @PutMapping
    public ResponseEntity<Void> updatePreference(@RequestBody PreferenceUpdateRequest request) {
        preferenceService.update(UserContext.get(), request);
        return ResponseEntity.ok().build();
    }
}
