package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-3002: Notification template CRUD + preview
 */
@DisplayName("R4-S4 :: BC-3002 — Notification Template CRUD")
class NotificationTemplateTest extends FunctionalTestBase {

    private static final String NT_TENANT = "nt-tenant-" + UUID.randomUUID();

    private String createTemplate(String type, String channel, String body) throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", NT_TENANT, "type", type, "channel", channel,
                "subject", type + " Subject", "bodyTemplate", body);
        String resp = POST("/v3/notifications/templates", req, bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("templateId").asText();
    }

    @Test
    @DisplayName("BC-3002 ✓ Create + list templates")
    void createAndList() throws Exception {
        String tenant = "nt-list-" + UUID.randomUUID();
        Map<String, Object> req = Map.of(
                "tenantId", tenant, "type", "ORDER_CONFIRMATION", "channel", "PUSH",
                "subject", "Subj", "bodyTemplate", "Your order {{orderNumber}} is confirmed.");
        POST("/v3/notifications/templates", req, bearer("admin1"))
                .andExpect(status().isCreated());

        GET("/v3/notifications/templates?tenantId=" + tenant, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("ORDER_CONFIRMATION")));
    }

    @Test
    @DisplayName("BC-3002 ✓ Get template by ID")
    void getById() throws Exception {
        String id = createTemplate("PRODUCTION_STARTED", "EMAIL", "Production started for {{orderNumber}}");

        GET("/v3/notifications/templates/" + id, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("PRODUCTION_STARTED")))
                .andExpect(jsonPath("$.channel", is("EMAIL")));
    }

    @Test
    @DisplayName("BC-3002 ✓ Update template")
    void updateTemplate() throws Exception {
        String id = createTemplate("READY_FOR_DELIVERY", "WHATSAPP", "Old body");

        PUT("/v3/notifications/templates/" + id,
                Map.of("subject", "Updated Subject", "bodyTemplate", "New body for {{customerName}}"),
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject", is("Updated Subject")))
                .andExpect(jsonPath("$.bodyTemplate", is("New body for {{customerName}}")));
    }

    @Test
    @DisplayName("BC-3002 ✓ Delete (soft) → active=false")
    void deleteTemplate() throws Exception {
        String id = createTemplate("DELIVERED", "SMS", "Delivered");

        DELETE("/v3/notifications/templates/" + id, bearer("admin1"))
                .andExpect(status().isNoContent());

        GET("/v3/notifications/templates/" + id, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    @DisplayName("BC-3002 ✓ Preview with variable substitution")
    void preview() throws Exception {
        String id = createTemplate("PAYMENT_REMINDER", "PUSH",
                "Hi {{customerName}}, order {{orderNumber}} status: {{status}}");

        POST("/v3/notifications/templates/" + id + "/preview",
                Map.of("variables", Map.of("customerName", "Armen", "orderNumber", "ORD-999", "status", "DELIVERED")),
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body", is("Hi Armen, order ORD-999 status: DELIVERED")));
    }

    @Test
    @DisplayName("BC-3002 ✓ Preview with default sample data")
    void previewDefaults() throws Exception {
        String id = createTemplate("STOCK_ALERT", "EMAIL",
                "Hello {{customerName}}, your order {{orderNumber}} is {{status}}");

        POST("/v3/notifications/templates/" + id + "/preview", Map.of(), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body", containsString("Sample Customer")))
                .andExpect(jsonPath("$.body", containsString("ORD-12345")));
    }

    @Test
    @DisplayName("BC-3002 ✓ 404 on unknown template ID")
    void notFound() throws Exception {
        GET("/v3/notifications/templates/nonexistent", bearer("admin1"))
                .andExpect(status().isNotFound());
    }
}
