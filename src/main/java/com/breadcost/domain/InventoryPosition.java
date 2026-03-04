package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * InventoryPosition entity
 * Current on-hand quantity by (siteId, itemId, lotId, locationId)
 * Derived from ledger entries in record order (ledgerSeq)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryPosition {
    private String siteId;
    private String itemId;
    private String lotId;
    private String locationId;
    private BigDecimal onHandQty;
    private Long rowVersion;
}
