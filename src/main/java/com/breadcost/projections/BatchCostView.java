package com.breadcost.projections;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * BatchCostView projection entity
 * Read model for batch cost analysis
 * Stored in H2 database for efficient querying
 */
@Entity
@Table(name = "batch_cost_view")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCostView {
    @Id
    private String batchId;
    
    private String siteId;
    private String status;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal totalMaterialCost;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal totalLaborCost;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal totalOverheadCost;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal totalCost;
    
    private Long lastProcessedLedgerSeq;
}
