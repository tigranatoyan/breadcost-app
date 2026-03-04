package com.breadcost.projections;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * InventoryValuationView projection entity
 * Read model for inventory valuation by item/lot/location
 */
@Entity
@Table(name = "inventory_valuation_view")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryValuationView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String siteId;
    private String itemId;
    private String lotId;
    private String locationId;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal onHandQty;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal valuationAmount;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal avgUnitCost;
    
    private Long lastProcessedLedgerSeq;
}
