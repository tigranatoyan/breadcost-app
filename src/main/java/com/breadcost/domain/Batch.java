package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Batch entity
 * Represents a production batch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Batch {
    private String batchId;
    private String siteId;
    private BatchStatus status;
    private Long recognitionCutoffSeq;

    public enum BatchStatus {
        CREATED,
        RELEASED,
        IN_PROGRESS,
        CLOSED
    }
}
