package com.breadcost.driver;

import com.breadcost.delivery.DeliveryRunEntity;
import com.breadcost.delivery.DeliveryRunOrderEntity;
import com.breadcost.delivery.DeliveryRunOrderRepository;
import com.breadcost.delivery.DeliveryRunRepository;
import com.breadcost.invoice.InvoiceEntity;
import com.breadcost.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Driver mobile service — BC-2101 (tracking), BC-2102 (packaging), BC-2103 (payment).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverSessionRepository sessionRepo;
    private final DriverStopUpdateRepository stopUpdateRepo;
    private final PackagingConfirmationRepository packagingRepo;
    private final DriverPaymentRepository paymentRepo;
    private final DeliveryRunRepository runRepo;
    private final DeliveryRunOrderRepository runOrderRepo;
    private final InvoiceRepository invoiceRepo;

    // ── BC-2101: Session + Tracking ──────────────────────────────────────────

    @Transactional
    public DriverSessionEntity startSession(String tenantId, String driverId,
                                             String driverName, String runId) {
        // Validate run exists
        runRepo.findByTenantIdAndRunId(tenantId, runId)
                .orElseThrow(() -> new NoSuchElementException("Delivery run not found: " + runId));

        // Close any existing active session for this driver
        sessionRepo.findByTenantIdAndDriverIdAndStatus(tenantId, driverId, "ACTIVE")
                .ifPresent(existing -> {
                    existing.setStatus("ENDED");
                    existing.setEndedAt(Instant.now());
                    sessionRepo.save(existing);
                });

        DriverSessionEntity session = DriverSessionEntity.builder()
                .sessionId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .driverId(driverId)
                .driverName(driverName)
                .runId(runId)
                .build();
        log.info("Driver session started: driver={} run={}", driverId, runId);
        return sessionRepo.save(session);
    }

    @Transactional
    public DriverSessionEntity updateLocation(String tenantId, String sessionId,
                                               double lat, double lng) {
        DriverSessionEntity session = sessionRepo.findById(sessionId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));
        session.setLat(lat);
        session.setLng(lng);
        return sessionRepo.save(session);
    }

    @Transactional
    public DriverSessionEntity endSession(String tenantId, String sessionId) {
        DriverSessionEntity session = sessionRepo.findById(sessionId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));
        session.setStatus("ENDED");
        session.setEndedAt(Instant.now());
        return sessionRepo.save(session);
    }

    /**
     * Get the manifest (list of stops/orders) for a driver's current session.
     */
    public List<DeliveryRunOrderEntity> getManifest(String tenantId, String sessionId) {
        DriverSessionEntity session = sessionRepo.findById(sessionId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));
        return runOrderRepo.findByRunId(session.getRunId());
    }

    /**
     * Mark a stop as delivered/failed (driver action).
     */
    @Transactional
    public DriverStopUpdateEntity updateStop(String tenantId, String sessionId,
                                              String runOrderId, String action,
                                              String notes, Double lat, Double lng) {
        DriverSessionEntity session = sessionRepo.findById(sessionId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));

        // Update the delivery run order status
        DeliveryRunOrderEntity runOrder = runOrderRepo.findById(runOrderId)
                .orElseThrow(() -> new NoSuchElementException("Run order not found: " + runOrderId));

        if ("DELIVERED".equalsIgnoreCase(action)) {
            runOrder.setStatus(DeliveryRunOrderEntity.OrderDeliveryStatus.COMPLETED);
            runOrder.setCompletedAt(Instant.now());
        } else if ("FAILED".equalsIgnoreCase(action)) {
            runOrder.setStatus(DeliveryRunOrderEntity.OrderDeliveryStatus.FAILED);
            runOrder.setFailureReason(notes);
        }
        runOrderRepo.save(runOrder);

        DriverStopUpdateEntity update = DriverStopUpdateEntity.builder()
                .updateId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .tenantId(tenantId)
                .runOrderId(runOrderId)
                .action(action.toUpperCase())
                .notes(notes)
                .lat(lat)
                .lng(lng)
                .build();

        log.info("Driver stop update: session={} order={} action={}", sessionId, runOrderId, action);
        return stopUpdateRepo.save(update);
    }

    public List<DriverSessionEntity> getActiveSessions(String tenantId) {
        return sessionRepo.findByTenantIdAndStatus(tenantId, "ACTIVE");
    }

    public List<DriverStopUpdateEntity> getStopUpdates(String sessionId) {
        return stopUpdateRepo.findBySessionId(sessionId);
    }

    // ── BC-2102: Packaging Confirmation ──────────────────────────────────────

    @Transactional
    public PackagingConfirmationEntity confirmPackaging(String tenantId, String runId,
                                                         String driverId, boolean allConfirmed,
                                                         String discrepancies) {
        // Check if confirmation already exists for this run
        Optional<PackagingConfirmationEntity> existing =
                packagingRepo.findByTenantIdAndRunId(tenantId, runId);
        if (existing.isPresent()) {
            PackagingConfirmationEntity conf = existing.get();
            conf.setAllConfirmed(allConfirmed);
            conf.setDiscrepancies(discrepancies);
            return packagingRepo.save(conf);
        }

        PackagingConfirmationEntity conf = PackagingConfirmationEntity.builder()
                .confirmationId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .runId(runId)
                .driverId(driverId)
                .allConfirmed(allConfirmed)
                .discrepancies(discrepancies)
                .build();

        if (!allConfirmed) {
            log.warn("Packaging discrepancies for run={}: {}", runId, discrepancies);
        }
        return packagingRepo.save(conf);
    }

    public Optional<PackagingConfirmationEntity> getPackagingConfirmation(String tenantId, String runId) {
        return packagingRepo.findByTenantIdAndRunId(tenantId, runId);
    }

    // ── BC-2103: On-Spot Payment Collection ──────────────────────────────────

    @Transactional
    public DriverPaymentEntity collectPayment(String tenantId, String sessionId,
                                               String orderId, BigDecimal amount,
                                               String paymentMethod, String reference) {
        DriverPaymentEntity payment = DriverPaymentEntity.builder()
                .paymentId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .sessionId(sessionId)
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod != null ? paymentMethod : "CASH")
                .reference(reference)
                .build();

        // Record payment against invoice if one exists
        invoiceRepo.findByTenantIdAndOrderId(tenantId, orderId).ifPresent(invoice -> {
            payment.setInvoiceId(invoice.getInvoiceId());
            invoice.setPaidAmount(amount);
            invoice.setPaidAt(Instant.now());
            invoice.setStatus(InvoiceEntity.InvoiceStatus.PAID);
            invoiceRepo.save(invoice);
        });

        log.info("Driver payment collected: order={} amount={} method={}", orderId, amount, paymentMethod);
        return paymentRepo.save(payment);
    }

    public List<DriverPaymentEntity> getPaymentsForSession(String sessionId) {
        return paymentRepo.findBySessionId(sessionId);
    }

    public List<DriverPaymentEntity> getPaymentsForOrder(String tenantId, String orderId) {
        return paymentRepo.findByTenantIdAndOrderId(tenantId, orderId);
    }
}
