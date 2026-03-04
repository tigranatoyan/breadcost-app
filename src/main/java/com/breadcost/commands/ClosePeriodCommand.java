package com.breadcost.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * ClosePeriod Command DTO
 * Closes accounting period after financial watermark conditions met
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClosePeriodCommand {
    @NotBlank
    private String tenantId;
    
    @NotBlank
    private String periodId;
    
    @NotNull
    private Instant occurredAtUtc;
    
    @NotBlank
    private String idempotencyKey;
    
    private String approvedBy;
}
