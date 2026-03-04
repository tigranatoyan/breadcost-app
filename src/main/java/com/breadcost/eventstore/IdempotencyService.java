package com.breadcost.eventstore;

import com.breadcost.domain.CommandIdempotency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency checker for commands
 * Tracks command execution to ensure exactly-once semantics
 */
@Service
@Slf4j
public class IdempotencyService {
    private final Map<String, CommandIdempotency> idempotencyRecords = new ConcurrentHashMap<>();

    /**
     * Check if command has already been executed
     * Returns existing result reference if found, null otherwise
     */
    public String checkIdempotency(String tenantId, String commandName, String idempotencyKey) {
        String key = buildKey(tenantId, commandName, idempotencyKey);
        CommandIdempotency record = idempotencyRecords.get(key);
        return record != null ? record.getResultRef() : null;
    }

    /**
     * Record command execution
     */
    public void recordExecution(String tenantId, String commandName, String idempotencyKey, String resultRef) {
        String key = buildKey(tenantId, commandName, idempotencyKey);
        CommandIdempotency record = CommandIdempotency.builder()
                .tenantId(tenantId)
                .commandName(commandName)
                .idempotencyKey(idempotencyKey)
                .createdAtUtc(Instant.now())
                .resultRef(resultRef)
                .build();
        
        idempotencyRecords.put(key, record);
        log.info("Recorded command execution: command={}, idempotencyKey={}, resultRef={}", 
                commandName, idempotencyKey, resultRef);
    }

    private String buildKey(String tenantId, String commandName, String idempotencyKey) {
        return tenantId + ":" + commandName + ":" + idempotencyKey;
    }
}
