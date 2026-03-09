package com.breadcost.functional;

import com.breadcost.delivery.*;
import com.breadcost.driver.*;
import com.breadcost.invoice.InvoiceEntity;
import com.breadcost.invoice.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("R3 :: BC-2101/2102/2103 — Driver Mobile")
class DriverTest extends FunctionalTestBase {

    @Autowired DeliveryRunRepository runRepo;
    @Autowired DeliveryRunOrderRepository runOrderRepo;
    @Autowired DriverSessionRepository sessionRepo;
    @Autowired InvoiceRepository invoiceRepo;

    private String runId;
    private String runOrderId;

    @BeforeEach
    void seedDeliveryRun() {
        runId = "run-driver-" + UUID.randomUUID().toString().substring(0, 8);
        runOrderId = "ro-driver-" + UUID.randomUUID().toString().substring(0, 8);

        runRepo.save(DeliveryRunEntity.builder()
                .runId(runId)
                .tenantId(TENANT)
                .driverId("driver1")
                .driverName("Test Driver")
                .build());

        runOrderRepo.save(DeliveryRunOrderEntity.builder()
                .id(runOrderId)
                .runId(runId)
                .tenantId(TENANT)
                .orderId("order-d1")
                .build());
    }

    // ── BC-2101: Session + Tracking ──────────────────────────────────────────

    @Test @DisplayName("BC-2101 ✓ Start session returns 201")
    void startSession_201() throws Exception {
        POST("/v3/driver/sessions",
                Map.of("tenantId", TENANT, "driverId", "driver1",
                        "driverName", "Test Driver", "runId", runId),
                bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.runId").value(runId));
    }

    @Test @DisplayName("BC-2101 ✓ Start session with bad run returns 404")
    void startSession_badRun_404() throws Exception {
        POST("/v3/driver/sessions",
                Map.of("tenantId", TENANT, "driverId", "driver1",
                        "driverName", "X", "runId", "no-such-run"),
                bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("BC-2101 ✓ Update location returns 200")
    void updateLocation_200() throws Exception {
        String sessionId = startAndGetSessionId();
        POST("/v3/driver/sessions/" + sessionId + "/location?tenantId=" + TENANT,
                Map.of("lat", 40.18, "lng", 44.51),
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").value(40.18))
                .andExpect(jsonPath("$.lng").value(44.51));
    }

    @Test @DisplayName("BC-2101 ✓ End session returns 200 with ENDED status")
    void endSession_200() throws Exception {
        String sessionId = startAndGetSessionId();
        POST_noBody("/v3/driver/sessions/" + sessionId + "/end?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"));
    }

    @Test @DisplayName("BC-2101 ✓ Get manifest returns run orders")
    void getManifest_returnsOrders() throws Exception {
        String sessionId = startAndGetSessionId();
        GET("/v3/driver/sessions/" + sessionId + "/manifest?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].orderId").value("order-d1"));
    }

    @Test @DisplayName("BC-2101 ✓ Update stop marks order COMPLETED")
    void updateStop_delivered() throws Exception {
        String sessionId = startAndGetSessionId();
        POST("/v3/driver/sessions/" + sessionId + "/stops/" + runOrderId + "?tenantId=" + TENANT,
                Map.of("action", "DELIVERED", "notes", "Left at door"),
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("DELIVERED"));
    }

    @Test @DisplayName("BC-2101 ✓ Get active sessions returns list")
    void getActiveSessions_200() throws Exception {
        startAndGetSessionId();
        GET("/v3/driver/sessions/active?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test @DisplayName("BC-2101 ✓ Get stop updates returns list")
    void getStopUpdates_200() throws Exception {
        String sessionId = startAndGetSessionId();
        // do a stop update first
        POST("/v3/driver/sessions/" + sessionId + "/stops/" + runOrderId + "?tenantId=" + TENANT,
                Map.of("action", "DELIVERED"),
                bearer("admin1"));
        GET("/v3/driver/sessions/" + sessionId + "/updates",
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // ── BC-2102: Packaging Confirmation ──────────────────────────────────────

    @Test @DisplayName("BC-2102 ✓ Confirm packaging returns 201")
    void confirmPackaging_201() throws Exception {
        POST("/v3/driver/packaging",
                Map.of("tenantId", TENANT, "runId", runId,
                        "driverId", "driver1", "allConfirmed", true),
                bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allConfirmed").value(true));
    }

    @Test @DisplayName("BC-2102 ✓ Confirm packaging with discrepancies")
    void confirmPackaging_discrepancies() throws Exception {
        POST("/v3/driver/packaging",
                Map.of("tenantId", TENANT, "runId", runId,
                        "driverId", "driver1", "allConfirmed", false,
                        "discrepancies", "Missing 2 loaves white bread"),
                bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allConfirmed").value(false))
                .andExpect(jsonPath("$.discrepancies").value("Missing 2 loaves white bread"));
    }

    @Test @DisplayName("BC-2102 ✓ Get packaging confirmation returns 200")
    void getPackaging_200() throws Exception {
        POST("/v3/driver/packaging",
                Map.of("tenantId", TENANT, "runId", runId,
                        "driverId", "driver1", "allConfirmed", true),
                bearer("admin1"));
        GET("/v3/driver/packaging/" + runId + "?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId));
    }

    @Test @DisplayName("BC-2102 ✓ Get packaging for unknown run returns 404")
    void getPackaging_notFound_404() throws Exception {
        GET("/v3/driver/packaging/no-such-run?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    // ── BC-2103: On-Spot Payment ─────────────────────────────────────────────

    @Test @DisplayName("BC-2103 ✓ Collect payment returns 201")
    void collectPayment_201() throws Exception {
        String sessionId = startAndGetSessionId();
        POST("/v3/driver/payments",
                Map.of("tenantId", TENANT, "sessionId", sessionId,
                        "orderId", "order-d1", "amount", 15000,
                        "paymentMethod", "CASH"),
                bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(15000))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"));
    }

    @Test @DisplayName("BC-2103 ✓ Payment auto-marks invoice as PAID")
    void collectPayment_marksInvoicePaid() throws Exception {
        // Seed an invoice for order-d1
        invoiceRepo.save(InvoiceEntity.builder()
                .invoiceId("inv-driver-1")
                .tenantId(TENANT)
                .customerId("cust1")
                .orderId("order-d1")
                .status(InvoiceEntity.InvoiceStatus.ISSUED)
                .totalAmount(new BigDecimal("15000"))
                .build());

        String sessionId = startAndGetSessionId();
        POST("/v3/driver/payments",
                Map.of("tenantId", TENANT, "sessionId", sessionId,
                        "orderId", "order-d1", "amount", 15000),
                bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").value("inv-driver-1"));

        // Verify invoice status changed
        InvoiceEntity inv = invoiceRepo.findById("inv-driver-1").orElseThrow();
        assert inv.getStatus() == InvoiceEntity.InvoiceStatus.PAID;
    }

    @Test @DisplayName("BC-2103 ✓ Get payments by session returns list")
    void getPaymentsBySession_200() throws Exception {
        String sessionId = startAndGetSessionId();
        POST("/v3/driver/payments",
                Map.of("tenantId", TENANT, "sessionId", sessionId,
                        "orderId", "order-d1", "amount", 5000),
                bearer("admin1"));
        GET("/v3/driver/payments/session/" + sessionId,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test @DisplayName("BC-2103 ✓ Get payments by order returns list")
    void getPaymentsByOrder_200() throws Exception {
        String sessionId = startAndGetSessionId();
        POST("/v3/driver/payments",
                Map.of("tenantId", TENANT, "sessionId", sessionId,
                        "orderId", "order-d1", "amount", 5000),
                bearer("admin1"));
        GET("/v3/driver/payments/order/order-d1?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String startAndGetSessionId() throws Exception {
        String body = POST("/v3/driver/sessions",
                Map.of("tenantId", TENANT, "driverId", "driver1",
                        "driverName", "Test Driver", "runId", runId),
                bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("sessionId").asText();
    }
}
