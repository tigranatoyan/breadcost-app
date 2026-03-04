package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * Period entity
 * Represents accounting periods (typically monthly)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Period {
    private String periodId;
    private String siteId;
    private LocalDate startDateLocal;
    private LocalDate endDateLocal;
    private PeriodStatus status;

    public enum PeriodStatus {
        OPEN,       // Normal operations
        CLOSING,    // Close initiated, waiting for projections
        LOCKED      // Period permanently closed
    }
}
