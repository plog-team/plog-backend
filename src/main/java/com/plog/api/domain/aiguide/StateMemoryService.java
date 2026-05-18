package com.plog.api.domain.aiguide;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.common.exception.BadRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StateMemoryService {

    private final UserStateMemoryRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void upsert(long userId, String key, Object valueObj) {
        String json;
        try {
            json = objectMapper.writeValueAsString(valueObj);
        } catch (Exception e) {
            throw new BadRequestException("state memory 직렬화 실패 key=" + key + ": " + e.getMessage());
        }
        Optional<UserStateMemory> existing = repository.findByUserIdAndMemoryKey(userId, key);
        if (existing.isPresent()) {
            existing.get().updateValue(json);
            log.debug("StateMemory UPDATE userId={} key={}", userId, key);
        } else {
            repository.save(UserStateMemory.builder()
                    .userId(userId)
                    .memoryKey(key)
                    .valueJson(json)
                    .build());
            log.debug("StateMemory INSERT userId={} key={}", userId, key);
        }
    }

    @Transactional(readOnly = true)
    public Optional<String> getRawJson(long userId, String key) {
        return repository.findByUserIdAndMemoryKey(userId, key).map(UserStateMemory::getValueJson);
    }

    @Transactional(readOnly = true)
    public <T> Optional<T> get(long userId, String key, Class<T> type) {
        return getRawJson(userId, key).map(json -> {
            try {
                return objectMapper.readValue(json, type);
            } catch (Exception e) {
                throw new BadRequestException("state memory 역직렬화 실패 key=" + key + ": " + e.getMessage());
            }
        });
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAllAsMap(long userId) {
        List<UserStateMemory> all = repository.findAllByUserId(userId);
        return all.stream().collect(java.util.stream.Collectors.toMap(
                UserStateMemory::getMemoryKey,
                UserStateMemory::getValueJson,
                (a, b) -> b));
    }
}
