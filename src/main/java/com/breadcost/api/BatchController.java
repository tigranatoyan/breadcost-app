package com.breadcost.api;

import com.breadcost.commands.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API for batch/production operations
 * Implements batch-related endpoints from API_SURFACE.yaml
 */
@Tag(name = "Batches", description = "Production batch cost tracking")
@RestController
@RequestMapping("/v1/batches")
@Slf4j
@RequiredArgsConstructor
public class BatchController {
    private final IssueToBatchCommandHandler issueToBatchHandler;

    /**
     * POST /v1/batches/{batchId}/issues
     * Issue inventory to batch
     */
    @PostMapping("/{batchId}/issues")
    @PreAuthorize("hasAnyRole('Admin', 'ProductionUser', 'ProductionSupervisor')")
    public ResponseEntity<CommandResult> issueToBatch(
            @PathVariable String batchId,
            @Valid @RequestBody IssueToBatchCommand command) {
        log.info("API: IssueToBatch - batchId={}, itemId={}", batchId, command.getItemId());
        command.setBatchId(batchId);
        CommandResult result = issueToBatchHandler.handle(command);
        return ResponseEntity.ok(result);
    }

    // Additional batch endpoints would be implemented here:
    // - POST /v1/batches (CreateBatch)
    // - POST /v1/batches/{batchId}/release (ReleaseBatch)
    // - POST /v1/batches/{batchId}/backflush (BackflushConsumption)
    // - POST /v1/batches/{batchId}/close (CloseBatch)
    // etc.
}
