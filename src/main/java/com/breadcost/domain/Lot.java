package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lot entity
 * Represents inventory lots tracked by (siteId, lotId) per LOT_REFERENCE_LAW
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lot {
    private String siteId;
    private String lotId;
    private String itemId;
    private LotClass lotClass;
    private String supplierLotCode;
    private LocalDate expiryDate;
    private Instant createdAtUtc;

    public enum LotClass {
        USER,
        SYSTEM      // e.g., __NO_LOT__ sentinel
    }
}
