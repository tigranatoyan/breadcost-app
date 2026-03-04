package com.breadcost.reporting;

import jakarta.persistence.*;
import lombok.*;

/**
 * A KPI block definition in the report catalog.
 * BC-1601: Report KPI block catalog.
 */
@Entity
@Table(name = "report_kpi_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportKpiBlockEntity {

    @Id
    private String blockId;

    /** e.g. "total_revenue", "cost_of_goods_sold", "gross_margin_pct" */
    @Column(nullable = false, unique = true)
    private String blockKey;

    @Column(nullable = false)
    private String name;

    private String description;

    /** e.g. FINANCIAL, PRODUCTION, DELIVERY, INVENTORY, CUSTOMER */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private KpiCategory category = KpiCategory.FINANCIAL;

    /** e.g. REVENUE, AGGREGATE, RATIO, COUNT */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QueryType queryType = QueryType.AGGREGATE;

    @Builder.Default
    private boolean active = true;

    private String unit; // e.g. "GBP", "%", "count"

    public enum KpiCategory {
        FINANCIAL, PRODUCTION, DELIVERY, INVENTORY, CUSTOMER
    }

    public enum QueryType {
        REVENUE, AGGREGATE, RATIO, COUNT, COMPARISON
    }
}
