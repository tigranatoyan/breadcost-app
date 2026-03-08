package com.breadcost.reporting;

import jakarta.persistence.*;
import lombok.*;

/**
 * Ordered KPI block within a custom report.
 * BC-1602
 */
@Entity
@Table(name = "custom_report_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomReportBlockEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String blockKey;

    @Builder.Default
    private int displayOrder = 0;

    // Optional date-range override for this block
    private String dateRangePreset; // e.g. "LAST_30_DAYS", "LAST_QUARTER", "CURRENT_MONTH"
}
