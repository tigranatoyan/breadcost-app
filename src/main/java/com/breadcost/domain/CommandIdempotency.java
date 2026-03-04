package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * CommandIdempotency entity
 * Tracks command execution for idempotency
 * Key: (tenantId, commandName, idempotencyKey)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandIdempotency {
    private String tenantId;
    private String commandName;
    private String idempotencyKey;
    private Instant createdAtUtc;
    private String resultRef;
}
