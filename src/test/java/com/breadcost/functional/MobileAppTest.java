package com.breadcost.functional;

import com.breadcost.mobile.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("R3 :: BC-2301 — Mobile Customer App")
class MobileAppTest extends FunctionalTestBase {

    @Autowired MobileDeviceRegistrationRepository deviceRepo;
    @Autowired PushNotificationRepository notificationRepo;

    // ── Device Registration ──────────────────────────────────────────────────

    @Test @DisplayName("BC-2301 ✓ Register device returns 201")
    void registerDevice_201() throws Exception {
        POST("/v3/mobile/devices",
                Map.of("tenantId", TENANT, "customerId", "cust-1",
                        "deviceToken", "tok-abc-123", "platform", "IOS",
                        "deviceName", "iPhone 15"),
                bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationId").isNotEmpty())
                .andExpect(jsonPath("$.platform").value("IOS"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test @DisplayName("BC-2301 ✓ Re-register same token updates existing")
    void registerDevice_reRegister() throws Exception {
        POST("/v3/mobile/devices",
                Map.of("tenantId", TENANT, "customerId", "cust-1",
                        "deviceToken", "tok-reregister", "platform", "ANDROID"),
                bearer("admin1"))
                .andExpect(status().isCreated());

        // Re-register same token
        POST("/v3/mobile/devices",
                Map.of("tenantId", TENANT, "customerId", "cust-2",
                        "deviceToken", "tok-reregister", "platform", "ANDROID"),
                bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("cust-2"));
    }

    @Test @DisplayName("BC-2301 ✓ Get devices returns list")
    void getDevices_200() throws Exception {
        POST("/v3/mobile/devices",
                Map.of("tenantId", TENANT, "customerId", "cust-dev-1",
                        "deviceToken", "tok-get-dev", "platform", "IOS"),
                bearer("admin1"));
        GET("/v3/mobile/devices?tenantId=" + TENANT + "&customerId=cust-dev-1",
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test @DisplayName("BC-2301 ✓ Unregister device returns 204")
    void unregisterDevice_204() throws Exception {
        String body = POST("/v3/mobile/devices",
                Map.of("tenantId", TENANT, "customerId", "cust-unreg",
                        "deviceToken", "tok-unreg", "platform", "ANDROID"),
                bearer("admin1"))
                .andReturn().getResponse().getContentAsString();
        String regId = om.readTree(body).get("registrationId").asText();

        DELETE("/v3/mobile/devices/" + regId + "?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isNoContent());
    }

    // ── Push Notifications ───────────────────────────────────────────────────

    @Test @DisplayName("BC-2301 ✓ Send notification returns 201")
    void sendNotification_201() throws Exception {
        POST("/v3/mobile/notifications",
                Map.of("tenantId", TENANT, "customerId", "cust-1",
                        "title", "Your order is ready",
                        "body", "Order #123 is ready for pickup",
                        "notificationType", "ORDER_STATUS",
                        "referenceId", "order-123"),
                bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Your order is ready"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test @DisplayName("BC-2301 ✓ Order status notification returns 201")
    void orderStatusNotification_201() throws Exception {
        POST("/v3/mobile/notifications/order-status",
                Map.of("tenantId", TENANT, "customerId", "cust-1",
                        "orderId", "order-456", "newStatus", "DELIVERED"),
                bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationType").value("ORDER_STATUS"))
                .andExpect(jsonPath("$.referenceId").value("order-456"));
    }

    @Test @DisplayName("BC-2301 ✓ Get notifications returns list")
    void getNotifications_200() throws Exception {
        POST("/v3/mobile/notifications",
                Map.of("tenantId", TENANT, "customerId", "cust-notif",
                        "title", "Test", "notificationType", "PROMOTION"),
                bearer("admin1"));
        GET("/v3/mobile/notifications?tenantId=" + TENANT + "&customerId=cust-notif",
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
