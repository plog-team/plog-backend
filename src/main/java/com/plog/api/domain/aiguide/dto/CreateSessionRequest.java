package com.plog.api.domain.aiguide.dto;

import java.util.List;

import com.plog.api.domain.aiguide.AiSession;
import com.plog.api.domain.aiguide.Persona;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
    @NotEmpty(message = "photoIds는 1개 이상 필요합니다")
    @Size(max = 10, message = "photoIds는 최대 10장까지 허용됩니다")
    List<Long> photoIds,

    AiSession.Mode mode,

    Persona persona
) {
    public AiSession.Mode modeOrDefault() {
        return mode == null ? AiSession.Mode.BATCH : mode;
    }

    public Persona personaOrDefault() {
        return Persona.orDefault(persona);
    }
}
