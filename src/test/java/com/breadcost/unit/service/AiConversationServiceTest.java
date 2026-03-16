package com.breadcost.unit.service;

import com.breadcost.ai.*;
import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerRepository;
import com.breadcost.masterdata.OrderService;
import com.breadcost.masterdata.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiConversationServiceTest {

    @Mock private AiConversationRepository conversationRepo;
    @Mock private AiMessageRepository messageRepo;
    @Mock private AiDraftOrderRepository draftOrderRepo;
    @Mock private AiDraftOrderLineRepository draftOrderLineRepo;
    @Mock private CustomerRepository customerRepo;
    @Mock private ProductRepository productRepo;
    @Mock private OrderService orderService;
    @InjectMocks private AiConversationService svc;

    // ── handleIncomingMessage — greeting ──────────────────────────────────────

    @Test
    void handleIncomingMessage_greeting_returnsWelcome() {
        var conv = AiConversationEntity.builder()
                .conversationId("c1").tenantId("t1").customerPhone("+37411111111").build();
        when(conversationRepo.findByTenantIdAndCustomerPhoneAndStatus("t1", "+37411111111", "ACTIVE"))
                .thenReturn(Optional.of(conv));
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepo.findByTenantIdAndPhone("t1", "+37411111111"))
                .thenReturn(Optional.of(CustomerEntity.builder().name("Armen").build()));

        var response = svc.handleIncomingMessage("t1", "+37411111111", "Hello!");

        assertNotNull(response);
        assertTrue(response.contains("Armen"));
    }

    @Test
    void handleIncomingMessage_armenianGreeting_works() {
        var conv = AiConversationEntity.builder()
                .conversationId("c1").tenantId("t1").customerPhone("+37411111111").build();
        when(conversationRepo.findByTenantIdAndCustomerPhoneAndStatus("t1", "+37411111111", "ACTIVE"))
                .thenReturn(Optional.of(conv));
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepo.findByTenantIdAndPhone("t1", "+37411111111"))
                .thenReturn(Optional.of(CustomerEntity.builder().name("Armen").build()));

        var response = svc.handleIncomingMessage("t1", "+37411111111", "Barev dzez");

        assertNotNull(response);
    }

    // ── handleIncomingMessage — escalated ─────────────────────────────────────

    @Test
    void handleIncomingMessage_escalated_returnsOperatorMsg() {
        var conv = AiConversationEntity.builder()
                .conversationId("c1").tenantId("t1").customerPhone("+37411111111")
                .escalated(true).build();
        when(conversationRepo.findByTenantIdAndCustomerPhoneAndStatus("t1", "+37411111111", "ACTIVE"))
                .thenReturn(Optional.of(conv));

        var response = svc.handleIncomingMessage("t1", "+37411111111", "Hello!");

        assertTrue(response.contains("operator"));
    }

    // ── handleIncomingMessage — new conversation ─────────────────────────────

    @Test
    void handleIncomingMessage_noExistingConv_createsNew() {
        when(conversationRepo.findByTenantIdAndCustomerPhoneAndStatus("t1", "+37400000000", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(conversationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepo.findByTenantIdAndPhone("t1", "+37400000000"))
                .thenReturn(Optional.empty());

        var response = svc.handleIncomingMessage("t1", "+37400000000", "Hello!");

        assertNotNull(response);
        verify(conversationRepo).save(any());
    }

    // ── handleIncomingMessage — help / escalation ────────────────────────────

    @Test
    void handleIncomingMessage_helpText_triggersEscalation() {
        var conv = AiConversationEntity.builder()
                .conversationId("c1").tenantId("t1").customerPhone("+37411111111").build();
        when(conversationRepo.findByTenantIdAndCustomerPhoneAndStatus("t1", "+37411111111", "ACTIVE"))
                .thenReturn(Optional.of(conv));
        when(conversationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = svc.handleIncomingMessage("t1", "+37411111111", "I need to speak to an operator");

        assertNotNull(response);
        assertTrue(conv.isEscalated());
    }

    // ── handleIncomingMessage — cancel ────────────────────────────────────────

    @Test
    void handleIncomingMessage_cancel_clearsDraft() {
        var conv = AiConversationEntity.builder()
                .conversationId("c1").tenantId("t1").customerPhone("+37411111111").build();
        when(conversationRepo.findByTenantIdAndCustomerPhoneAndStatus("t1", "+37411111111", "ACTIVE"))
                .thenReturn(Optional.of(conv));
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(draftOrderRepo.findByConversationIdAndStatus("c1", "PENDING_CONFIRMATION"))
                .thenReturn(Optional.<AiDraftOrderEntity>empty());

        var response = svc.handleIncomingMessage("t1", "+37411111111", "cancel");

        assertNotNull(response);
    }

    // ── escalateToHuman ──────────────────────────────────────────────────────

    @Test
    void escalateToHuman_setsEscalatedFlag() {
        var conv = AiConversationEntity.builder()
                .conversationId("c1").tenantId("t1").build();
        when(conversationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.escalateToHuman(conv, "t1", "test reason");

        assertTrue(conv.isEscalated());
        assertEquals("ESCALATED", conv.getStatus());
        assertEquals("test reason", conv.getEscalationReason());
    }

    // ── resolveEscalation ────────────────────────────────────────────────────

    @Test
    void resolveEscalation_close_completesConversation() {
        var conv = AiConversationEntity.builder()
                .conversationId("c1").tenantId("t1").escalated(true).status("ESCALATED").build();
        when(conversationRepo.findById("c1")).thenReturn(Optional.of(conv));
        when(conversationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.resolveEscalation("t1", "c1", true);

        assertFalse(conv.isEscalated());
        assertEquals("COMPLETED", conv.getStatus());
    }

    @Test
    void resolveEscalation_resume_setsActive() {
        var conv = AiConversationEntity.builder()
                .conversationId("c1").tenantId("t1").escalated(true).status("ESCALATED").build();
        when(conversationRepo.findById("c1")).thenReturn(Optional.of(conv));
        when(conversationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.resolveEscalation("t1", "c1", false);

        assertEquals("ACTIVE", conv.getStatus());
    }

    @Test
    void resolveEscalation_wrongTenant_throws() {
        var conv = AiConversationEntity.builder()
                .conversationId("c1").tenantId("t2").build();
        when(conversationRepo.findById("c1")).thenReturn(Optional.of(conv));

        assertThrows(NoSuchElementException.class,
                () -> svc.resolveEscalation("t1", "c1", false));
    }

    @Test
    void resolveEscalation_notFound_throws() {
        when(conversationRepo.findById("bad")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> svc.resolveEscalation("t1", "bad", false));
    }

    // ── listConversations / listEscalated ────────────────────────────────────

    @Test
    void listConversations_delegatesToRepo() {
        var conv = AiConversationEntity.builder()
                .conversationId("c1").tenantId("t1").build();
        when(conversationRepo.findByTenantId("t1")).thenReturn(List.of(conv));

        var result = svc.listConversations("t1");

        assertEquals(1, result.size());
    }

    @Test
    void listEscalated_delegatesToRepo() {
        when(conversationRepo.findByTenantIdAndStatus("t1", "ESCALATED")).thenReturn(List.of());

        var result = svc.listEscalated("t1");

        assertTrue(result.isEmpty());
    }
}
