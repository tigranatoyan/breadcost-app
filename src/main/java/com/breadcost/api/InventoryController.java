package com.breadcost.api;

import com.breadcost.commands.*;
import com.breadcost.projections.InventoryProjection;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for inventory operations
 * Implements endpoints from API_SURFACE.yaml
 */
@RestController
@RequestMapping("/v1/inventory")
@Slf4j
@RequiredArgsConstructor
public class InventoryController {
    private final ReceiveLotCommandHandler receiveLotHandler;
    private final TransferInventoryCommandHandler transferInventoryHandler;
    private final InventoryProjection inventoryProjection;

    /**
     * GET /v1/inventory/positions
     * List current on-hand stock positions from projection
     */
    @GetMapping("/positions")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser','FinanceUser','Viewer')")
    public ResponseEntity<List<InventoryProjection.InventoryPosition>> getPositions(
            @RequestParam String tenantId,
            @RequestParam(required = false) String siteId) {

        List<InventoryProjection.InventoryPosition> all = siteId != null
                ? inventoryProjection.getPositionsBySite(siteId)
                : inventoryProjection.getAllPositions();

        List<InventoryProjection.InventoryPosition> filtered = all.stream()
                .filter(p -> tenantId.equals(p.getTenantId()))
                .toList();

        return ResponseEntity.ok(filtered);
    }

    /**
     * POST /v1/inventory/receipts
     * Receive inventory lot
     */
    @PostMapping("/receipts")
    @PreAuthorize("hasAnyRole('Admin', 'InventoryController')")
    public ResponseEntity<CommandResult> receiveLot(@Valid @RequestBody ReceiveLotCommand command) {
        log.info("API: ReceiveLot - receiptId={}", command.getReceiptId());
        CommandResult result = receiveLotHandler.handle(command);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /v1/inventory/transfers
     * Transfer inventory between locations
     */
    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('Admin', 'InventoryController', 'ProductionUser')")
    public ResponseEntity<CommandResult> transferInventory(@Valid @RequestBody TransferInventoryCommand command) {
        log.info("API: TransferInventory - itemId={}", command.getItemId());
        CommandResult result = transferInventoryHandler.handle(command);
        return ResponseEntity.ok(result);
    }
}
