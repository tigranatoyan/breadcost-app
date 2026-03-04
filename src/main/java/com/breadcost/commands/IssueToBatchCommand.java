package com.breadcost.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * IssueToBatch Command DTO
 * Issues inventory to production batch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueToBatchCommand {
    @NotBlank
    private String tenantId;
    
    @NotBlank
    private String siteId;
    
    @NotBlank
    private String batchId;
    
    @NotBlank
    private String itemId;
    
    @NotNull
    private BigDecimal qty;
    
    @NotBlank
    private String uom;
    
    private String lotId;
    private String locationId;
    
    @NotNull
    private Instant occurredAtUtc;
    
    @NotBlank
    private String idempotencyKey;
    
    private String overrideReasonCode;
    private Boolean emergencyMode;
    private String approvedBy;
    private String approvalRef;
    private String exceptionId;
}
