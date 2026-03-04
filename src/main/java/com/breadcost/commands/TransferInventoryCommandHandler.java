package com.breadcost.commands;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.TransferInventoryEvent;
import com.breadcost.eventstore.EventStore;
import com.breadcost.eventstore.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handler for TransferInventory command
 * Produces OPERATIONAL ledger entry (amountBase=0)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransferInventoryCommandHandler {
    private final EventStore eventStore;
    private final IdempotencyService idempotencyService;

    public CommandResult handle(TransferInventoryCommand command) {
        log.info("Handling TransferInventory command: itemId={}, from={}, to={}", 
                command.getItemId(), command.getFromLocationId(), command.getToLocationId());

        // Check idempotency
        String existingResult = idempotencyService.checkIdempotency(
                command.getTenantId(), "TransferInventory", command.getIdempotencyKey());
        if (existingResult != null) {
            return CommandResult.builder()
                    .success(true)
                    .resultRef(existingResult)
                    .message("Command already executed (idempotent)")
                    .build();
        }

        // Validate
        validateCommand(command);

        // Create and emit event
        TransferInventoryEvent event = TransferInventoryEvent.builder()
                .tenantId(command.getTenantId())
                .siteId(command.getSiteId())
                .itemId(command.getItemId())
                .qty(command.getQty())
                .fromLocationId(command.getFromLocationId())
                .toLocationId(command.getToLocationId())
                .occurredAtUtc(command.getOccurredAtUtc())
                .idempotencyKey(command.getIdempotencyKey())
                .lotId(command.getLotId())
                .build();

        // OPERATIONAL entry per POSTING_RULES (amountBase=0)
        Long ledgerSeq = eventStore.appendEvent(event, LedgerEntry.EntryClass.OPERATIONAL);
        String resultRef = "transfer:seq:" + ledgerSeq;

        idempotencyService.recordExecution(
                command.getTenantId(), "TransferInventory", command.getIdempotencyKey(), resultRef);

        return CommandResult.builder()
                .success(true)
                .resultRef(resultRef)
                .ledgerSeq(ledgerSeq)
                .message("Transfer recorded successfully")
                .build();
    }

    private void validateCommand(TransferInventoryCommand command) {
        if (command.getQty().signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (command.getFromLocationId().equals(command.getToLocationId())) {
            throw new IllegalArgumentException("From and To locations must be different");
        }
    }
}
