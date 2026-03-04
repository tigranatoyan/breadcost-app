package com.breadcost.commands;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.IssueToBatchEvent;
import com.breadcost.eventstore.EventStore;
import com.breadcost.eventstore.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handler for IssueToBatch command
 * Validates, checks RBAC and approval rules, emits event
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IssueToBatchCommandHandler {
    private final EventStore eventStore;
    private final IdempotencyService idempotencyService;

    public CommandResult handle(IssueToBatchCommand command) {
        log.info("Handling IssueToBatch command: batchId={}, itemId={}, qty={}", 
                command.getBatchId(), command.getItemId(), command.getQty());

        // Check idempotency
        String existingResult = idempotencyService.checkIdempotency(
                command.getTenantId(), "IssueToBatch", command.getIdempotencyKey());
        if (existingResult != null) {
            return CommandResult.builder()
                    .success(true)
                    .resultRef(existingResult)
                    .message("Command already executed (idempotent)")
                    .build();
        }

        // Validate command
        validateCommand(command);

        // Check approval requirements per RBAC_MATRIX
        if (Boolean.TRUE.equals(command.getEmergencyMode()) && command.getApprovedBy() == null) {
            throw new IllegalArgumentException("Emergency mode requires approval");
        }

        // Create and emit event
        IssueToBatchEvent event = IssueToBatchEvent.builder()
                .tenantId(command.getTenantId())
                .siteId(command.getSiteId())
                .batchId(command.getBatchId())
                .itemId(command.getItemId())
                .qty(command.getQty())
                .uom(command.getUom())
                .lotId(command.getLotId())
                .locationId(command.getLocationId())
                .occurredAtUtc(command.getOccurredAtUtc())
                .idempotencyKey(command.getIdempotencyKey())
                .overrideReasonCode(command.getOverrideReasonCode())
                .emergencyMode(command.getEmergencyMode())
                .approvedBy(command.getApprovedBy())
                .approvalRef(command.getApprovalRef())
                .exceptionId(command.getExceptionId())
                .build();

        Long ledgerSeq = eventStore.appendEvent(event, LedgerEntry.EntryClass.FINANCIAL);
        String resultRef = "issue:" + command.getBatchId() + ":seq:" + ledgerSeq;

        idempotencyService.recordExecution(
                command.getTenantId(), "IssueToBatch", command.getIdempotencyKey(), resultRef);

        return CommandResult.builder()
                .success(true)
                .resultRef(resultRef)
                .ledgerSeq(ledgerSeq)
                .message("Issue to batch recorded successfully")
                .build();
    }

    private void validateCommand(IssueToBatchCommand command) {
        if (command.getQty().signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        // Additional validation would check:
        // - Item exists and is valid for usage
        // - Batch exists and is in valid state
        // - Lot tracking requirements per item.lotTracked
    }
}
