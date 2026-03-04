package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

/**
 * RecognitionOutputSet entity
 * Immutable snapshot of outputs recognized when batch closes
 * Per RECOGNITION_OUTPUT_SET_ID_LAW
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionOutputSet {
    private String recognitionOutputSetId;
    private String tenantId;
    private String siteId;
    private String batchId;
    private Long recognitionCutoffSeq;
    private Instant createdAtUtc;
    private String allocationBasis;
    private List<Output> outputs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        private String itemId;
        private String qty;
        private String uom;
        private String lotId;
    }
}
