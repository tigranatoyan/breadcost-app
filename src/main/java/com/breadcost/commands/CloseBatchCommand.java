package com.breadcost.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * CloseBatch Command DTO
 * Closes production batch and triggers output recognition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseBatchCommand {
    @NotBlank
    private String tenantId;
    
    @NotBlank
    private String siteId;
    
    @NotBlank
    private String batchId;
    
    @NotBlank
    private String closeMode;
    
    @NotNull
    private Instant occurredAtUtc;
    
    @NotBlank
    private String idempotencyKey;
}
