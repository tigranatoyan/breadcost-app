package com.breadcost.unit.service;

import com.breadcost.delivery.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRunRepository runRepo;
    @Mock private DeliveryRunOrderRepository orderRepo;
    @InjectMocks private DeliveryService svc;

    // ── createRun ────────────────────────────────────────────────────────────

    @Test
    void createRun_setsFieldsAndSaves() {
        when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var run = svc.createRun("t1", "d1", "John", LocalDate.of(2025, 1, 15),
                new BigDecimal("5.00"), "notes");

        assertNotNull(run.getRunId());
        assertEquals("t1", run.getTenantId());
        assertEquals("d1", run.getDriverId());
        assertEquals(new BigDecimal("5.00"), run.getCourierCharge());
    }

    @Test
    void createRun_nullCourierCharge_defaultsToZero() {
        when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var run = svc.createRun("t1", "d1", "John", LocalDate.now(), null, null);

        assertEquals(BigDecimal.ZERO, run.getCourierCharge());
    }

    // ── assignOrder ──────────────────────────────────────────────────────────

    @Test
    void assignOrder_verifiesRunExistsAndRecalculates() {
        var existingRun = DeliveryRunEntity.builder()
                .runId("run1").tenantId("t1").courierCharge(BigDecimal.TEN).build();
        when(runRepo.findByTenantIdAndRunId("t1", "run1")).thenReturn(Optional.of(existingRun));
        when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepo.findByRunId("run1")).thenReturn(List.of(
                DeliveryRunOrderEntity.builder().id("o1").runId("run1").build()));

        var result = svc.assignOrder("t1", "run1", "order1");

        assertEquals("order1", result.getOrderId());
        assertEquals("run1", result.getRunId());
    }

    @Test
    void assignOrder_unknownRun_throws() {
        when(runRepo.findByTenantIdAndRunId("t1", "bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.assignOrder("t1", "bad", "order1"));
    }

    // ── markOrderCompleted ───────────────────────────────────────────────────

    @Test
    void markOrderCompleted_updatesStatusAndTime() {
        var dro = DeliveryRunOrderEntity.builder().id("x1").runId("run1").orderId("o1")
                .status(DeliveryRunOrderEntity.OrderDeliveryStatus.PENDING).build();
        when(orderRepo.findByRunIdAndOrderId("run1", "o1")).thenReturn(Optional.of(dro));
        when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepo.findByRunId("run1")).thenReturn(List.of(dro));
        when(runRepo.findByTenantIdAndRunId("t1", "run1"))
                .thenReturn(Optional.of(DeliveryRunEntity.builder().runId("run1").tenantId("t1").build()));

        var result = svc.markOrderCompleted("t1", "run1", "o1");

        assertEquals(DeliveryRunOrderEntity.OrderDeliveryStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void markOrderCompleted_allDone_completesRun() {
        var dro = DeliveryRunOrderEntity.builder().id("x1").runId("run1").orderId("o1")
                .status(DeliveryRunOrderEntity.OrderDeliveryStatus.PENDING).build();
        var run = DeliveryRunEntity.builder().runId("run1").tenantId("t1")
                .status(DeliveryRunEntity.RunStatus.PENDING).build();

        when(orderRepo.findByRunIdAndOrderId("run1", "o1")).thenReturn(Optional.of(dro));
        when(orderRepo.save(any())).thenAnswer(inv -> {
            DeliveryRunOrderEntity e = inv.getArgument(0);
            return e;
        });
        // After save, the dro status is COMPLETED, so allDone = true
        when(orderRepo.findByRunId("run1")).thenReturn(List.of(dro));
        when(runRepo.findByTenantIdAndRunId("t1", "run1")).thenReturn(Optional.of(run));
        when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.markOrderCompleted("t1", "run1", "o1");

        assertEquals(DeliveryRunEntity.RunStatus.COMPLETED, run.getStatus());
    }

    // ── markOrderFailed ──────────────────────────────────────────────────────

    @Test
    void markOrderFailed_setsStatusAndReason() {
        var dro = DeliveryRunOrderEntity.builder().id("x1").runId("run1").orderId("o1")
                .status(DeliveryRunOrderEntity.OrderDeliveryStatus.PENDING).build();
        when(orderRepo.findByRunIdAndOrderId("run1", "o1")).thenReturn(Optional.of(dro));
        when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.markOrderFailed("t1", "run1", "o1", "customer absent");

        assertEquals(DeliveryRunOrderEntity.OrderDeliveryStatus.FAILED, result.getStatus());
        assertEquals("customer absent", result.getFailureReason());
    }

    // ── reAssignOrder ────────────────────────────────────────────────────────

    @Test
    void reAssignOrder_marksOriginalFailedAndCreatesNew() {
        var original = DeliveryRunOrderEntity.builder().id("x1").runId("run1").orderId("o1")
                .status(DeliveryRunOrderEntity.OrderDeliveryStatus.PENDING).build();
        var newRun = DeliveryRunEntity.builder().runId("run2").tenantId("t1")
                .courierCharge(BigDecimal.ZERO).build();

        when(orderRepo.findByRunIdAndOrderId("run1", "o1")).thenReturn(Optional.of(original));
        when(runRepo.findByTenantIdAndRunId("t1", "run2")).thenReturn(Optional.of(newRun));
        when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepo.findByRunId("run2")).thenReturn(List.of());

        var result = svc.reAssignOrder("t1", "run1", "o1", "run2");

        assertEquals(DeliveryRunOrderEntity.OrderDeliveryStatus.FAILED, original.getStatus());
        assertEquals("run2", original.getReDeliveryRunId());
        assertEquals("run2", result.getRunId());
    }

    // ── recalculateCourierSplit ──────────────────────────────────────────────

    @Test
    void recalculateCourierSplit_equalSplit() {
        var run = DeliveryRunEntity.builder().runId("r1").tenantId("t1")
                .courierCharge(new BigDecimal("9.00")).build();
        var o1 = DeliveryRunOrderEntity.builder().id("d1").runId("r1").build();
        var o2 = DeliveryRunOrderEntity.builder().id("d2").runId("r1").build();
        var o3 = DeliveryRunOrderEntity.builder().id("d3").runId("r1").build();

        when(runRepo.findByTenantIdAndRunId("t1", "r1")).thenReturn(Optional.of(run));
        when(orderRepo.findByRunId("r1")).thenReturn(List.of(o1, o2, o3));
        when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.recalculateCourierSplit("t1", "r1");

        assertEquals(new BigDecimal("3.00"), o1.getCourierCharge());
        assertEquals(new BigDecimal("3.00"), o2.getCourierCharge());
        assertEquals(new BigDecimal("3.00"), o3.getCourierCharge());
    }

    @Test
    void recalculateCourierSplit_skipsWaivedOrders() {
        var run = DeliveryRunEntity.builder().runId("r1").tenantId("t1")
                .courierCharge(new BigDecimal("6.00")).build();
        var o1 = DeliveryRunOrderEntity.builder().id("d1").runId("r1").build();
        var o2 = DeliveryRunOrderEntity.builder().id("d2").runId("r1").courierChargeWaived(true).build();

        when(runRepo.findByTenantIdAndRunId("t1", "r1")).thenReturn(Optional.of(run));
        when(orderRepo.findByRunId("r1")).thenReturn(List.of(o1, o2));
        when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.recalculateCourierSplit("t1", "r1");

        // Split is 6/2 = 3.00 but waived order keeps its existing charge
        assertEquals(new BigDecimal("3.00"), o1.getCourierCharge());
    }

    @Test
    void recalculateCourierSplit_zeroCharge_noOp() {
        var run = DeliveryRunEntity.builder().runId("r1").tenantId("t1")
                .courierCharge(BigDecimal.ZERO).build();

        when(runRepo.findByTenantIdAndRunId("t1", "r1")).thenReturn(Optional.of(run));
        when(orderRepo.findByRunId("r1")).thenReturn(List.of(
                DeliveryRunOrderEntity.builder().id("d1").runId("r1").build()));

        svc.recalculateCourierSplit("t1", "r1");

        verify(orderRepo, never()).save(any());
    }

    // ── waiveCourierCharge ───────────────────────────────────────────────────

    @Test
    void waiveCourierCharge_setsWaivedFlagAndZeroCharge() {
        var dro = DeliveryRunOrderEntity.builder().id("x1").runId("run1").orderId("o1")
                .courierCharge(new BigDecimal("5.00")).build();
        when(orderRepo.findByRunIdAndOrderId("run1", "o1")).thenReturn(Optional.of(dro));
        when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.waiveCourierCharge("t1", "run1", "o1", "manager1");

        assertTrue(result.isCourierChargeWaived());
        assertEquals("manager1", result.getWaivedBy());
        assertEquals(BigDecimal.ZERO, result.getCourierCharge());
    }

    // ── getManifest ──────────────────────────────────────────────────────────

    @Test
    void getManifest_returnsRunAndOrders() {
        var run = DeliveryRunEntity.builder().runId("r1").tenantId("t1").build();
        var orders = List.of(
                DeliveryRunOrderEntity.builder().id("d1").runId("r1").orderId("o1").build());
        when(runRepo.findByTenantIdAndRunId("t1", "r1")).thenReturn(Optional.of(run));
        when(orderRepo.findByRunId("r1")).thenReturn(orders);

        var manifest = svc.getManifest("t1", "r1");

        assertEquals("r1", manifest.run().getRunId());
        assertEquals(1, manifest.orders().size());
    }
}
