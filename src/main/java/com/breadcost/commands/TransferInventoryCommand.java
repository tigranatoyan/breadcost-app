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
 * TransferInventory Command DTO
 * Transfers inventory between locations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferInventoryCommand {
    @NotBlank
    private String tenantId;
    
    @NotBlank
    private String siteId;
    
    @NotBlank
    private String itemId;
    
    @NotNull
    private BigDecimal qty;
    
    @NotBlank
    private String fromLocationId;
    
    @NotBlank
    private String toLocationId;
    
    @NotNull
    private Instant occurredAtUtc;
    
    @NotBlank
    private String idempotencyKey;
    
    private String lotId;
}
