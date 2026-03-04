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
 * ReceiveLot Command DTO
 * Records receipt of inventory lot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveLotCommand {
    @NotBlank
    private String tenantId;
    
    @NotBlank
    private String siteId;
    
    @NotBlank
    private String receiptId;
    
    @NotBlank
    private String itemId;
    
    @NotBlank
    private String lotId;
    
    @NotNull
    private BigDecimal qty;
    
    @NotBlank
    private String uom;
    
    @NotNull
    private BigDecimal unitCostBase;
    
    @NotNull
    private Instant occurredAtUtc;
    
    @NotBlank
    private String idempotencyKey;
}
