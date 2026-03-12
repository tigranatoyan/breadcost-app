package com.breadcost.api;

import com.breadcost.ai.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import com.breadcost.subscription.SubscriptionRequired;

/**
 * AI WhatsApp ordering endpoints — BC-1801..1804 (FR-12.x)
 */
@RestController
@RequestMapping("/v3/ai")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin','Manager')")
@SubscriptionRequired("AI_BOT")
public class AiConversationController {

    private final AiConversationService service;

    // ── Webhook: inbound message from WhatsApp ───────────────────────────────

    @PostMapping("/webhook/whatsapp")
    @PreAuthorize("permitAll()")
    public Map<String, String> receiveMessage(@RequestBody @Valid InboundMessage req) {
        String reply = service.handleIncomingMessage(req.tenantId, req.phone, req.text);
        return Map.of("reply", reply);
    }

    @Data
    static class InboundMessage {
        @NotBlank String tenantId;
        @NotBlank String phone;
        @NotBlank String text;
    }

    // ── Conversation queries ─────────────────────────────────────────────────

    @GetMapping("/conversations")
    public List<AiConversationEntity> listConversations(@RequestParam String tenantId) {
        return service.listConversations(tenantId);
    }

    @GetMapping("/conversations/escalated")
    public List<AiConversationEntity> listEscalated(@RequestParam String tenantId) {
        return service.listEscalated(tenantId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<AiMessageEntity> getMessages(@PathVariable String conversationId) {
        return service.getMessages(conversationId);
    }

    @GetMapping("/conversations/{conversationId}/drafts")
    public List<AiDraftOrderEntity> getDrafts(@PathVariable String conversationId) {
        return service.getDrafts(conversationId);
    }

    // ── Escalation management ────────────────────────────────────────────────

    @PostMapping("/conversations/{conversationId}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolveEscalation(@RequestParam String tenantId,
                                   @PathVariable String conversationId,
                                   @RequestParam(defaultValue = "false") boolean close) {
        service.resolveEscalation(tenantId, conversationId, close);
    }
}
