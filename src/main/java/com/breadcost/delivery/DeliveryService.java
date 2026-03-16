package com.breadcost.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Delivery service — BC-E14
 *
 * BC-1401: Assign orders to delivery runs
 * BC-1402: Delivery manifest generation
 * BC-1403: Mark delivery completed
 * BC-1404: Failed delivery recording and re-delivery workflow
 * BC-1405: Split delivery courier charge calculation
 * BC-1406: Courier charge waiver with authorisation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRunRepository runRepository;
    private final DeliveryRunOrderRepository orderRepository;

    // ── BC-1401: Create run + assign orders ───────────────────────────────────

    @Transactional
    public DeliveryRunEntity createRun(String tenantId, String driverId, String driverName,
                                        LocalDate scheduledDate, BigDecimal courierCharge,
                                        String notes) {
        DeliveryRunEntity run = DeliveryRunEntity.builder()
                .runId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .driverId(driverId)
                .driverName(driverName)
                .scheduledDate(scheduledDate)
                .courierCharge(courierCharge != null ? courierCharge : BigDecimal.ZERO)
                .notes(notes)
                .runNumber(runRepository.maxRunNumber(tenantId) + 1)
                .build();
        log.info("Creating delivery run: tenantId={} driver={}", tenantId, driverId);
        return runRepository.save(run);
    }

    @Transactional
    public DeliveryRunOrderEntity assignOrder(String tenantId, String runId, String orderId) {
        return doAssignOrder(tenantId, runId, orderId);
    }

    private DeliveryRunOrderEntity doAssignOrder(String tenantId, String runId, String orderId) {
        getRunOrThrow(tenantId, runId);  // verify run exists

        DeliveryRunOrderEntity dro = DeliveryRunOrderEntity.builder()
                .id(UUID.randomUUID().toString())
                .runId(runId)
                .tenantId(tenantId)
                .orderId(orderId)
                .build();

        // Recalculate split charges after adding
        DeliveryRunOrderEntity saved = orderRepository.save(dro);
        recalculateCourierSplit(tenantId, runId);
        return saved;
    }

    public List<DeliveryRunOrderEntity> getRunOrders(String runId) {
        return orderRepository.findByRunId(runId);
    }

    public List<DeliveryRunEntity> listRuns(String tenantId) {
        return runRepository.findByTenantId(tenantId);
    }

    public DeliveryRunEntity getRun(String tenantId, String runId) {
        return getRunOrThrow(tenantId, runId);
    }

    // ── BC-1402: Manifest ─────────────────────────────────────────────────────

    /**
     * Returns manifest data (run + orders) for generating a printable document.
     */
    public record ManifestResult(DeliveryRunEntity run, List<DeliveryRunOrderEntity> orders) {}

    public ManifestResult getManifest(String tenantId, String runId) {
        DeliveryRunEntity run = getRunOrThrow(tenantId, runId);
        List<DeliveryRunOrderEntity> orders = orderRepository.findByRunId(runId);
        return new ManifestResult(run, orders);
    }

    // ── BC-1403: Mark completed ───────────────────────────────────────────────

    @Transactional
    public DeliveryRunOrderEntity markOrderCompleted(String tenantId, String runId, String orderId) {
        DeliveryRunOrderEntity dro = getRunOrderOrThrow(runId, orderId);
        dro.setStatus(DeliveryRunOrderEntity.OrderDeliveryStatus.COMPLETED);
        dro.setCompletedAt(Instant.now());
        log.info("Order completed in delivery: runId={} orderId={}", runId, orderId);

        DeliveryRunOrderEntity saved = orderRepository.save(dro);
        maybeCompleteRun(tenantId, runId);
        return saved;
    }

    // ── BC-1404: Failed delivery + re-delivery ────────────────────────────────

    @Transactional
    public DeliveryRunOrderEntity markOrderFailed(String tenantId, String runId,
                                                   String orderId, String failureReason) {
        DeliveryRunOrderEntity dro = getRunOrderOrThrow(runId, orderId);
        dro.setStatus(DeliveryRunOrderEntity.OrderDeliveryStatus.FAILED);
        dro.setFailureReason(failureReason);
        log.info("Order failed: runId={} orderId={} reason={}", runId, orderId, failureReason);
        return orderRepository.save(dro);
    }

    @Transactional
    public DeliveryRunOrderEntity reAssignOrder(String tenantId, String originalRunId,
                                                 String orderId, String newRunId) {
        // Mark the original as failed (if not already)
        DeliveryRunOrderEntity original = getRunOrderOrThrow(originalRunId, orderId);
        if (original.getStatus() != DeliveryRunOrderEntity.OrderDeliveryStatus.FAILED) {
            original.setStatus(DeliveryRunOrderEntity.OrderDeliveryStatus.FAILED);
            orderRepository.save(original);
        }
        original.setReDeliveryRunId(newRunId);
        orderRepository.save(original);

        // Assign to new run
        return doAssignOrder(tenantId, newRunId, orderId);
    }

    // ── BC-1405: Split courier charge ─────────────────────────────────────────

    /**
     * Recalculate courier charge per order as equal split from run total.
     */
    public void recalculateCourierSplit(String tenantId, String runId) {
        DeliveryRunEntity run = getRunOrThrow(tenantId, runId);
        List<DeliveryRunOrderEntity> orders = orderRepository.findByRunId(runId);
        if (orders.isEmpty() || run.getCourierCharge().compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal split = run.getCourierCharge()
                .divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);

        for (DeliveryRunOrderEntity o : orders) {
            if (!o.isCourierChargeWaived()) {
                o.setCourierCharge(split);
                orderRepository.save(o);
            }
        }
    }

    // ── BC-1406: Courier charge waiver ────────────────────────────────────────

    @Transactional
    public DeliveryRunOrderEntity waiveCourierCharge(String tenantId, String runId,
                                                      String orderId, String waivedBy) {
        DeliveryRunOrderEntity dro = getRunOrderOrThrow(runId, orderId);
        dro.setCourierChargeWaived(true);
        dro.setWaivedBy(waivedBy);
        dro.setCourierCharge(BigDecimal.ZERO);
        log.info("Courier charge waived: runId={} orderId={} by={}", runId, orderId, waivedBy);
        return orderRepository.save(dro);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DeliveryRunEntity getRunOrThrow(String tenantId, String runId) {
        return runRepository.findByTenantIdAndRunId(tenantId, runId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery run not found: " + runId));
    }

    private DeliveryRunOrderEntity getRunOrderOrThrow(String runId, String orderId) {
        return orderRepository.findByRunIdAndOrderId(runId, orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not in run: runId=" + runId + " orderId=" + orderId));
    }

    private void maybeCompleteRun(String tenantId, String runId) {
        List<DeliveryRunOrderEntity> orders = orderRepository.findByRunId(runId);
        boolean allDone = orders.stream().allMatch(o ->
                o.getStatus() == DeliveryRunOrderEntity.OrderDeliveryStatus.COMPLETED
                || o.getStatus() == DeliveryRunOrderEntity.OrderDeliveryStatus.FAILED);
        if (allDone) {
            runRepository.findByTenantIdAndRunId(tenantId, runId).ifPresent(run -> {
                run.setStatus(DeliveryRunEntity.RunStatus.COMPLETED);
                runRepository.save(run);
            });
        }
    }
}
