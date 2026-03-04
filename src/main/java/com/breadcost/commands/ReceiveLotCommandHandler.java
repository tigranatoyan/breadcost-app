package com.breadcost.commands;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.ReceiveLotEvent;
import com.breadcost.eventstore.EventStore;
import com.breadcost.eventstore.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handler for ReceiveLot command
 * Validates, checks idempotency, emits event
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiveLotCommandHandler {
    private final EventStore eventStore;
    private final IdempotencyService idempotencyService;

    public CommandResult handle(ReceiveLotCommand command) {
        log.info("Handling ReceiveLot command: receiptId={}, itemId={}, lotId={}", 
                command.getReceiptId(), command.getItemId(), command.getLotId());

        // Check idempotency
        String existingResult = idempotencyService.checkIdempotency(
                command.getTenantId(), "ReceiveLot", command.getIdempotencyKey());
        if (existingResult != null) {
            log.info("Command already executed, returning existing result: {}", existingResult);
            return CommandResult.builder()
                    .success(true)
                    .resultRef(existingResult)
                    .message("Command already executed (idempotent)")
                    .build();
        }

        // Validate command
        validateCommand(command);

        // Create and emit event
        ReceiveLotEvent event = ReceiveLotEvent.builder()
                .tenantId(command.getTenantId())
                .siteId(command.getSiteId())
                .receiptId(command.getReceiptId())
                .itemId(command.getItemId())
                .lotId(command.getLotId())
                .qty(command.getQty())
                .uom(command.getUom())
                .unitCostBase(command.getUnitCostBase())
                .occurredAtUtc(command.getOccurredAtUtc())
                .idempotencyKey(command.getIdempotencyKey())
                .build();

        Long ledgerSeq = eventStore.appendEvent(event, LedgerEntry.EntryClass.FINANCIAL);
        String resultRef = "receipt:" + command.getReceiptId() + ":seq:" + ledgerSeq;

        // Record idempotency
        idempotencyService.recordExecution(
                command.getTenantId(), "ReceiveLot", command.getIdempotencyKey(), resultRef);

        log.info("ReceiveLot command handled successfully: ledgerSeq={}", ledgerSeq);
        return CommandResult.builder()
                .success(true)
                .resultRef(resultRef)
                .ledgerSeq(ledgerSeq)
                .message("Receipt recorded successfully")
                .build();
    }

    private void validateCommand(ReceiveLotCommand command) {
        if (command.getQty().signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (command.getUnitCostBase().signum() < 0) {
            throw new IllegalArgumentException("Unit cost cannot be negative");
        }
        // Additional validation per MASTERDATA_VALIDATION_RULES would go here
    }
}
