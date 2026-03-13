package com.breadcost.api;

import com.breadcost.delivery.DeliveryRunOrderEntity;
import com.breadcost.driver.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import com.breadcost.subscription.SubscriptionRequired;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Driver mobile app endpoints — BC-2101..2103 (FR-7.7, FR-8.7, FR-8.8)
 */
@Tag(name = "Drivers", description = "Driver management, assignments, and performance")
@RestController
@RequestMapping("/v3/driver")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin','Manager')")
@SubscriptionRequired("DELIVERY")
public class DriverController {

    private final DriverService service;

    // ── BC-2101: Session + Tracking ──────────────────────────────────────────

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public DriverSessionEntity startSession(@RequestBody @Valid StartSessionRequest req) {
        return service.startSession(req.tenantId, req.driverId, req.driverName, req.runId);
    }

    @PostMapping("/sessions/{sessionId}/location")
    public DriverSessionEntity updateLocation(@RequestParam String tenantId,
                                               @PathVariable String sessionId,
                                               @RequestBody @Valid LocationUpdate loc) {
        return service.updateLocation(tenantId, sessionId, loc.lat, loc.lng);
    }

    @PostMapping("/sessions/{sessionId}/end")
    public DriverSessionEntity endSession(@RequestParam String tenantId,
                                           @PathVariable String sessionId) {
        return service.endSession(tenantId, sessionId);
    }

    @GetMapping("/sessions/{sessionId}/manifest")
    public List<DeliveryRunOrderEntity> getManifest(@RequestParam String tenantId,
                                                     @PathVariable String sessionId) {
        return service.getManifest(tenantId, sessionId);
    }

    @PostMapping("/sessions/{sessionId}/stops/{runOrderId}")
    public DriverStopUpdateEntity updateStop(@RequestParam String tenantId,
                                              @PathVariable String sessionId,
                                              @PathVariable String runOrderId,
                                              @RequestBody @Valid StopUpdateRequest req) {
        return service.updateStop(tenantId, sessionId, runOrderId,
                req.action, req.notes, req.lat, req.lng);
    }

    @GetMapping("/sessions/active")
    public List<DriverSessionEntity> getActiveSessions(@RequestParam String tenantId) {
        return service.getActiveSessions(tenantId);
    }

    @GetMapping("/sessions/{sessionId}/updates")
    public List<DriverStopUpdateEntity> getStopUpdates(@PathVariable String sessionId) {
        return service.getStopUpdates(sessionId);
    }

    @Data
    static class StartSessionRequest {
        @NotBlank String tenantId;
        @NotBlank String driverId;
        String driverName;
        @NotBlank String runId;
    }

    @Data
    static class LocationUpdate {
        @NotNull Double lat;
        @NotNull Double lng;
    }

    @Data
    static class StopUpdateRequest {
        @NotBlank String action;
        String notes;
        Double lat;
        Double lng;
    }

    // ── BC-2102: Packaging Confirmation ──────────────────────────────────────

    @PostMapping("/packaging")
    @ResponseStatus(HttpStatus.CREATED)
    public PackagingConfirmationEntity confirmPackaging(@RequestBody @Valid PackagingRequest req) {
        return service.confirmPackaging(req.tenantId, req.runId, req.driverId,
                req.allConfirmed, req.discrepancies);
    }

    @GetMapping("/packaging/{runId}")
    public PackagingConfirmationEntity getPackaging(@RequestParam String tenantId,
                                                     @PathVariable String runId) {
        return service.getPackagingConfirmation(tenantId, runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No packaging confirmation for run " + runId));
    }

    @Data
    static class PackagingRequest {
        @NotBlank String tenantId;
        @NotBlank String runId;
        @NotBlank String driverId;
        boolean allConfirmed;
        String discrepancies;
    }

    // ── BC-2103: On-Spot Payment ─────────────────────────────────────────────

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public DriverPaymentEntity collectPayment(@RequestBody @Valid PaymentRequest req) {
        return service.collectPayment(req.tenantId, req.sessionId, req.orderId,
                req.amount, req.paymentMethod, req.reference);
    }

    @GetMapping("/payments/session/{sessionId}")
    public List<DriverPaymentEntity> getSessionPayments(@PathVariable String sessionId) {
        return service.getPaymentsForSession(sessionId);
    }

    @GetMapping("/payments/order/{orderId}")
    public List<DriverPaymentEntity> getOrderPayments(@RequestParam String tenantId,
                                                       @PathVariable String orderId) {
        return service.getPaymentsForOrder(tenantId, orderId);
    }

    @Data
    static class PaymentRequest {
        @NotBlank String tenantId;
        String sessionId;
        @NotBlank String orderId;
        @NotNull BigDecimal amount;
        String paymentMethod;
        String reference;
    }
}
