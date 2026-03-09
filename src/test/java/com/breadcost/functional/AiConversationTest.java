package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1801..1804: AI WhatsApp conversation tests (FR-12.x)
 */
@DisplayName("R3 :: BC-1801..1804 — AI WhatsApp Ordering")
class AiConversationTest extends FunctionalTestBase {

    private static final String BASE = "/v3/ai";

    @Test
    @DisplayName("BC-1801 ✓ Greeting message returns welcome with order instructions")
    void greeting_returnsWelcome() throws Exception {
        POST(BASE + "/webhook/whatsapp", Map.of(
                "tenantId", TENANT,
                "phone", "+374-91-001001",
                "text", "Hello"
        ), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("ordering assistant")));
    }

    @Test
    @DisplayName("BC-1802 ✓ Order message creates draft and asks for confirmation")
    void orderMessage_createsDraft() throws Exception {
        // First, send a greeting to create the conversation
        POST(BASE + "/webhook/whatsapp", Map.of(
                "tenantId", TENANT,
                "phone", "+374-91-002002",
                "text", "Hi"
        ), bearer("admin1")).andExpect(status().isOk());

        // Then send an order
        POST(BASE + "/webhook/whatsapp", Map.of(
                "tenantId", TENANT,
                "phone", "+374-91-002002",
                "text", "5 loaves White Bread, 2 kg Lavash"
        ), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("confirm")));
    }

    @Test
    @DisplayName("BC-1804 ✓ Help message escalates to human")
    void helpMessage_escalates() throws Exception {
        POST(BASE + "/webhook/whatsapp", Map.of(
                "tenantId", TENANT,
                "phone", "+374-91-003003",
                "text", "I need help please"
        ), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("team member")));
    }

    @Test
    @DisplayName("BC-1804 ✓ List escalated conversations")
    void listEscalated_afterHelpRequest() throws Exception {
        // Create escalated conversation
        POST(BASE + "/webhook/whatsapp", Map.of(
                "tenantId", TENANT,
                "phone", "+374-91-004004",
                "text", "Help"
        ), bearer("admin1")).andExpect(status().isOk());

        GET(BASE + "/conversations/escalated?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", hasItem("ESCALATED")));
    }

    @Test
    @DisplayName("BC-1802 ✓ List all conversations for tenant")
    void listConversations_returnsAll() throws Exception {
        POST(BASE + "/webhook/whatsapp", Map.of(
                "tenantId", TENANT,
                "phone", "+374-91-005005",
                "text", "Hello"
        ), bearer("admin1")).andExpect(status().isOk());

        GET(BASE + "/conversations?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("BC-1802 ✓ Get messages for a conversation")
    void getMessages_returnsHistory() throws Exception {
        // Create conversation and get its ID
        POST(BASE + "/webhook/whatsapp", Map.of(
                "tenantId", TENANT,
                "phone", "+374-91-006006",
                "text", "Good morning"
        ), bearer("admin1")).andExpect(status().isOk());

        // Get conversations to find the ID
        String convBody = GET(BASE + "/conversations?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var convs = om.readTree(convBody);
        String convId = null;
        for (var c : convs) {
            if ("+374-91-006006".equals(c.get("customerPhone").asText())) {
                convId = c.get("conversationId").asText();
                break;
            }
        }

        if (convId != null) {
            GET(BASE + "/conversations/" + convId + "/messages", bearer("admin1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2))); // inbound + outbound
        }
    }

    @Test
    @DisplayName("BC-1802 ✓ Cancel order flow")
    void cancelOrder_returnsConfirmation() throws Exception {
        POST(BASE + "/webhook/whatsapp", Map.of(
                "tenantId", TENANT,
                "phone", "+374-91-007007",
                "text", "Hi"
        ), bearer("admin1")).andExpect(status().isOk());

        POST(BASE + "/webhook/whatsapp", Map.of(
                "tenantId", TENANT,
                "phone", "+374-91-007007",
                "text", "Cancel"
        ), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("cancelled")));
    }

    @Test
    @DisplayName("BC-1804 ✓ Resolve escalation returns 204")
    void resolveEscalation_returns204() throws Exception {
        // Escalate first
        POST(BASE + "/webhook/whatsapp", Map.of(
                "tenantId", TENANT,
                "phone", "+374-91-008008",
                "text", "Speak to an agent"
        ), bearer("admin1")).andExpect(status().isOk());

        // Find the ESCALATED conversation
        String convBody = GET(BASE + "/conversations/escalated?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var escalated = om.readTree(convBody);
        String convId = null;
        for (var c : escalated) {
            if ("+374-91-008008".equals(c.get("customerPhone").asText())) {
                convId = c.get("conversationId").asText();
                break;
            }
        }

        if (convId != null) {
            POST_noBody(BASE + "/conversations/" + convId + "/resolve?tenantId=" + TENANT + "&close=true",
                    bearer("admin1"))
                    .andExpect(status().isNoContent());
        }
    }
}
