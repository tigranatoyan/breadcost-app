package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * ExceptionCase entity
 * Tracks business exceptions requiring resolution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionCase {
    private String exceptionId;
    private String siteId;
    private String type;
    private String severity;
    private String status;
    private String relatedRef;
    private Instant createdAtUtc;
    private Instant resolvedAtUtc;
}
