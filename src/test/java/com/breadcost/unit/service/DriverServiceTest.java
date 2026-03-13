package com.breadcost.unit.service;

import com.breadcost.delivery.*;
import com.breadcost.driver.*;
import com.breadcost.invoice.InvoiceEntity;
import com.breadcost.invoice.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock private DriverSessionRepository sessionRepo;
    @Mock private DriverStopUpdateRepository stopUpdateRepo;
    @Mock private PackagingConfirmationRepository packagingRepo;
    @Mock private DriverPaymentRepository paymentRepo;
    @Mock private DeliveryRunRepository runRepo;
    @Mock private DeliveryRunOrderRepository runOrderRepo;
    @Mock private InvoiceRepository invoiceRepo;
    @InjectMocks private DriverService svc;

    // ── startSession ─────────────────────────────────────────────────────────

    @Test
    void startSession_createsNewSession() {
        var run = DeliveryRunEntity.builder().runId("r1").tenantId("t1").build();
        when(runRepo.findByTenantIdAndRunId("t1", "r1")).thenReturn(Optional.of(run));
        when(sessionRepo.findByTenantIdAndDriverIdAndStatus("t1", "d1", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.startSession("t1", "d1", "Driver One", "r1");

        assertNotNull(result.getSessionId());
        assertEquals("t1", result.getTenantId());
        assertEquals("d1", result.getDriverId());
        assertEquals("r1", result.getRunId());
    }

    @Test
    void startSession_endsExistingActiveSession() {
        var run = DeliveryRunEntity.builder().runId("r1").tenantId("t1").build();
        when(runRepo.findByTenantIdAndRunId("t1", "r1")).thenReturn(Optional.of(run));
        var existing = DriverSessionEntity.builder()
                .sessionId("old").tenantId("t1").driverId("d1").status("ACTIVE").build();
        when(sessionRepo.findByTenantIdAndDriverIdAndStatus("t1", "d1", "ACTIVE"))
                .thenReturn(Optional.of(existing));
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.startSession("t1", "d1", "Driver One", "r1");

        assertEquals("ENDED", existing.getStatus());
        assertNotNull(existing.getEndedAt());
    }

    @Test
    void startSession_runNotFound_throws() {
        when(runRepo.findByTenantIdAndRunId("t1", "bad")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> svc.startSession("t1", "d1", "Driver", "bad"));
    }

    // ── updateLocation ───────────────────────────────────────────────────────

    @Test
    void updateLocation_setsLatLng() {
        var session = DriverSessionEntity.builder()
                .sessionId("s1").tenantId("t1").status("ACTIVE").build();
        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.updateLocation("t1", "s1", 40.18, 44.51);

        assertEquals(40.18, result.getLat());
        assertEquals(44.51, result.getLng());
    }

    @Test
    void updateLocation_wrongTenant_throws() {
        var session = DriverSessionEntity.builder()
                .sessionId("s1").tenantId("other").build();
        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));

        assertThrows(NoSuchElementException.class,
                () -> svc.updateLocation("t1", "s1", 0, 0));
    }

    // ── endSession ───────────────────────────────────────────────────────────

    @Test
    void endSession_setsStatusEnded() {
        var session = DriverSessionEntity.builder()
                .sessionId("s1").tenantId("t1").status("ACTIVE").build();
        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.endSession("t1", "s1");

        assertEquals("ENDED", result.getStatus());
        assertNotNull(result.getEndedAt());
    }

    // ── getManifest ──────────────────────────────────────────────────────────

    @Test
    void getManifest_returnsRunOrders() {
        var session = DriverSessionEntity.builder()
                .sessionId("s1").tenantId("t1").runId("r1").build();
        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        var orders = List.of(DeliveryRunOrderEntity.builder()
                .id("ro1").runId("r1").orderId("o1").build());
        when(runOrderRepo.findByRunId("r1")).thenReturn(orders);

        var result = svc.getManifest("t1", "s1");

        assertEquals(1, result.size());
        assertEquals("o1", result.get(0).getOrderId());
    }

    // ── updateStop ───────────────────────────────────────────────────────────

    @Test
    void updateStop_delivered_setsCompleted() {
        var session = DriverSessionEntity.builder()
                .sessionId("s1").tenantId("t1").build();
        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        var runOrder = DeliveryRunOrderEntity.builder()
                .id("ro1").runId("r1").orderId("o1")
                .status(DeliveryRunOrderEntity.OrderDeliveryStatus.PENDING).build();
        when(runOrderRepo.findById("ro1")).thenReturn(Optional.of(runOrder));
        when(runOrderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stopUpdateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.updateStop("t1", "s1", "ro1", "DELIVERED", null, 40.0, 44.0);

        assertEquals("DELIVERED", result.getAction());
        assertEquals(DeliveryRunOrderEntity.OrderDeliveryStatus.COMPLETED, runOrder.getStatus());
    }

    @Test
    void updateStop_failed_setsFailedWithReason() {
        var session = DriverSessionEntity.builder()
                .sessionId("s1").tenantId("t1").build();
        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        var runOrder = DeliveryRunOrderEntity.builder()
                .id("ro1").runId("r1").orderId("o1")
                .status(DeliveryRunOrderEntity.OrderDeliveryStatus.PENDING).build();
        when(runOrderRepo.findById("ro1")).thenReturn(Optional.of(runOrder));
        when(runOrderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stopUpdateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.updateStop("t1", "s1", "ro1", "FAILED", "Not home", 40.0, 44.0);

        assertEquals(DeliveryRunOrderEntity.OrderDeliveryStatus.FAILED, runOrder.getStatus());
        assertEquals("Not home", runOrder.getFailureReason());
    }

    // ── confirmPackaging ─────────────────────────────────────────────────────

    @Test
    void confirmPackaging_newConfirmation() {
        when(packagingRepo.findByTenantIdAndRunId("t1", "r1")).thenReturn(Optional.empty());
        when(packagingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.confirmPackaging("t1", "r1", "d1", true, null);

        assertTrue(result.isAllConfirmed());
        assertEquals("t1", result.getTenantId());
    }

    @Test
    void confirmPackaging_updatesExisting() {
        var existing = PackagingConfirmationEntity.builder()
                .confirmationId("c1").tenantId("t1").runId("r1")
                .driverId("d1").allConfirmed(true).build();
        when(packagingRepo.findByTenantIdAndRunId("t1", "r1")).thenReturn(Optional.of(existing));
        when(packagingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.confirmPackaging("t1", "r1", "d1", false, "Missing 2 items");

        assertFalse(result.isAllConfirmed());
        assertEquals("Missing 2 items", result.getDiscrepancies());
    }

    // ── collectPayment ───────────────────────────────────────────────────────

    @Test
    void collectPayment_withInvoice_marksInvoicePaid() {
        var invoice = InvoiceEntity.builder()
                .invoiceId("inv1").tenantId("t1").orderId("o1")
                .customerId("c1").status(InvoiceEntity.InvoiceStatus.ISSUED).build();
        when(invoiceRepo.findByTenantIdAndOrderId("t1", "o1")).thenReturn(Optional.of(invoice));
        when(invoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.collectPayment("t1", "s1", "o1",
                new BigDecimal("5000"), "CASH", null);

        assertEquals("inv1", result.getInvoiceId());
        assertEquals(InvoiceEntity.InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(new BigDecimal("5000"), invoice.getPaidAmount());
    }

    @Test
    void collectPayment_noInvoice_stillSavesPayment() {
        when(invoiceRepo.findByTenantIdAndOrderId("t1", "o1")).thenReturn(Optional.empty());
        when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.collectPayment("t1", "s1", "o1",
                new BigDecimal("3000"), null, "ref123");

        assertNotNull(result.getPaymentId());
        assertEquals("CASH", result.getPaymentMethod()); // default
        assertNull(result.getInvoiceId());
    }
}
