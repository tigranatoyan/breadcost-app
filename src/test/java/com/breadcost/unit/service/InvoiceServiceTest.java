package com.breadcost.unit.service;

import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerDiscountRuleRepository;
import com.breadcost.customers.CustomerRepository;
import com.breadcost.invoice.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepo;
    @Mock private InvoiceLineRepository invoiceLineRepo;
    @Mock private CustomerRepository customerRepo;
    @Mock private CustomerDiscountRuleRepository discountRuleRepo;

    @InjectMocks private InvoiceService invoiceService;

    private InvoiceEntity draftInvoice;
    private InvoiceEntity issuedInvoice;
    private InvoiceEntity disputedInvoice;

    @BeforeEach
    void setUp() {
        draftInvoice = InvoiceEntity.builder()
                .invoiceId("inv-1").tenantId("t1").customerId("cust-1").orderId("ord-1")
                .status(InvoiceEntity.InvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        issuedInvoice = InvoiceEntity.builder()
                .invoiceId("inv-2").tenantId("t1").customerId("cust-1").orderId("ord-2")
                .status(InvoiceEntity.InvoiceStatus.ISSUED)
                .totalAmount(new BigDecimal("200.00"))
                .build();

        disputedInvoice = InvoiceEntity.builder()
                .invoiceId("inv-3").tenantId("t1").customerId("cust-1").orderId("ord-3")
                .status(InvoiceEntity.InvoiceStatus.DISPUTED)
                .disputeReason("Damaged goods")
                .disputedAt(Instant.now())
                .totalAmount(new BigDecimal("150.00"))
                .build();
    }

    // ── issueInvoice tests ───────────────────────────────────────────────

    @Test
    void issueInvoice_draftInvoice_setsIssued() {
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-1")).thenReturn(Optional.of(draftInvoice));
        when(invoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceEntity result = invoiceService.issueInvoice("t1", "inv-1");

        assertEquals(InvoiceEntity.InvoiceStatus.ISSUED, result.getStatus());
    }

    @Test
    void issueInvoice_alreadyIssued_throwsException() {
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-2")).thenReturn(Optional.of(issuedInvoice));

        assertThrows(IllegalStateException.class,
                () -> invoiceService.issueInvoice("t1", "inv-2"));
    }

    // ── markPaid tests ───────────────────────────────────────────────────

    @Test
    void markPaid_issuedInvoice_setsPaid() {
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-2")).thenReturn(Optional.of(issuedInvoice));
        when(invoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepo.findById("cust-1")).thenReturn(Optional.of(
                CustomerEntity.builder().customerId("cust-1").tenantId("t1")
                        .outstandingBalance(new BigDecimal("200.00")).build()));

        InvoiceEntity result = invoiceService.markPaid("t1", "inv-2", new BigDecimal("200.00"), "finance-user");

        assertEquals(InvoiceEntity.InvoiceStatus.PAID, result.getStatus());
        assertNotNull(result.getPaidAt());
    }

    @Test
    void markPaid_alreadyPaid_throwsException() {
        InvoiceEntity paidInvoice = InvoiceEntity.builder()
                .invoiceId("inv-p").tenantId("t1")
                .status(InvoiceEntity.InvoiceStatus.PAID).build();
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-p")).thenReturn(Optional.of(paidInvoice));

        assertThrows(IllegalStateException.class,
                () -> invoiceService.markPaid("t1", "inv-p", BigDecimal.TEN, "user"));
    }

    @Test
    void markPaid_cancelledInvoice_throwsException() {
        InvoiceEntity cancelled = InvoiceEntity.builder()
                .invoiceId("inv-c").tenantId("t1")
                .status(InvoiceEntity.InvoiceStatus.CANCELLED).build();
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-c")).thenReturn(Optional.of(cancelled));

        assertThrows(IllegalStateException.class,
                () -> invoiceService.markPaid("t1", "inv-c", BigDecimal.TEN, "user"));
    }

    // ── markOverdue tests ────────────────────────────────────────────────

    @Test
    void markOverdue_issuedInvoice_setsOverdue() {
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-2")).thenReturn(Optional.of(issuedInvoice));
        when(invoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceEntity result = invoiceService.markOverdue("t1", "inv-2");

        assertEquals(InvoiceEntity.InvoiceStatus.OVERDUE, result.getStatus());
    }

    @Test
    void markOverdue_draftInvoice_throwsException() {
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-1")).thenReturn(Optional.of(draftInvoice));

        assertThrows(IllegalStateException.class,
                () -> invoiceService.markOverdue("t1", "inv-1"));
    }

    // ── disputeInvoice tests ─────────────────────────────────────────────

    @Test
    void disputeInvoice_issuedInvoice_setsDisputed() {
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-2")).thenReturn(Optional.of(issuedInvoice));
        when(invoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceEntity result = invoiceService.disputeInvoice("t1", "inv-2", "Wrong quantity", "cust-user");

        assertEquals(InvoiceEntity.InvoiceStatus.DISPUTED, result.getStatus());
        assertEquals("Wrong quantity", result.getDisputeReason());
        assertNotNull(result.getDisputedAt());
    }

    @Test
    void disputeInvoice_draftInvoice_throwsException() {
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-1")).thenReturn(Optional.of(draftInvoice));

        assertThrows(IllegalStateException.class,
                () -> invoiceService.disputeInvoice("t1", "inv-1", "Reason", "user"));
    }

    // ── resolveDispute tests ─────────────────────────────────────────────

    @Test
    void resolveDispute_withCreditNote_reducesBalance() {
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-3")).thenReturn(Optional.of(disputedInvoice));
        when(invoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CustomerEntity customer = CustomerEntity.builder()
                .customerId("cust-1").tenantId("t1")
                .outstandingBalance(new BigDecimal("300.00")).build();
        when(customerRepo.findById("cust-1")).thenReturn(Optional.of(customer));

        InvoiceEntity result = invoiceService.resolveDispute("t1", "inv-3",
                "Agreed on partial credit", new BigDecimal("50.00"));

        assertEquals(new BigDecimal("50.00"), result.getCreditNoteAmount());
        assertNotNull(result.getResolvedAt());
        verify(customerRepo).save(argThat(c -> c.getOutstandingBalance().compareTo(new BigDecimal("250.00")) == 0));
    }

    @Test
    void resolveDispute_nonDisputedInvoice_throwsException() {
        when(invoiceRepo.findByTenantIdAndInvoiceId("t1", "inv-2")).thenReturn(Optional.of(issuedInvoice));

        assertThrows(IllegalStateException.class,
                () -> invoiceService.resolveDispute("t1", "inv-2", "Notes", BigDecimal.ZERO));
    }

    // ── hasSufficientCredit tests ────────────────────────────────────────

    @Test
    void hasSufficientCredit_withinLimit_returnsTrue() {
        CustomerEntity customer = CustomerEntity.builder()
                .customerId("cust-1").tenantId("t1")
                .creditLimit(new BigDecimal("1000.00"))
                .outstandingBalance(new BigDecimal("500.00")).build();
        when(customerRepo.findById("cust-1")).thenReturn(Optional.of(customer));

        assertTrue(invoiceService.hasSufficientCredit("t1", "cust-1", new BigDecimal("400.00")));
    }

    @Test
    void hasSufficientCredit_exceedsLimit_returnsFalse() {
        CustomerEntity customer = CustomerEntity.builder()
                .customerId("cust-1").tenantId("t1")
                .creditLimit(new BigDecimal("1000.00"))
                .outstandingBalance(new BigDecimal("800.00")).build();
        when(customerRepo.findById("cust-1")).thenReturn(Optional.of(customer));

        assertFalse(invoiceService.hasSufficientCredit("t1", "cust-1", new BigDecimal("300.00")));
    }

    @Test
    void hasSufficientCredit_zeroCreditLimit_alwaysTrue() {
        CustomerEntity customer = CustomerEntity.builder()
                .customerId("cust-1").tenantId("t1")
                .creditLimit(BigDecimal.ZERO)
                .outstandingBalance(new BigDecimal("5000.00")).build();
        when(customerRepo.findById("cust-1")).thenReturn(Optional.of(customer));

        assertTrue(invoiceService.hasSufficientCredit("t1", "cust-1", new BigDecimal("10000.00")));
    }

    @Test
    void hasSufficientCredit_unknownCustomer_returnsFalse() {
        when(customerRepo.findById("unknown")).thenReturn(Optional.empty());

        assertFalse(invoiceService.hasSufficientCredit("t1", "unknown", BigDecimal.TEN));
    }
}
