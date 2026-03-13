package com.breadcost.invoice;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Invoice generated from a delivered/completed customer order.
 * BC-E15: Invoicing & Credit
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceEntity {

    @Id
    private String invoiceId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String customerId;

    /** Source order that triggered invoice generation. */
    @Column(nullable = false)
    private String orderId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    /** ISO-8601 invoice number, e.g. INV-2024-00001 */
    private String invoiceNumber;

    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String currencyCode;

    /** Days until payment is due (copied from customer at invoice creation). */
    @Builder.Default
    private int paymentTermsDays = 30;

    private LocalDate issuedDate;
    private LocalDate dueDate;

    private Instant paidAt;
    private String paidBy;
    private BigDecimal paidAmount;

    private String notes;

    // ── G-4: Invoice dispute fields ─────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String disputeReason;
    private Instant disputedAt;
    private String disputedBy;
    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;
    private Instant resolvedAt;
    /** Credit note amount if dispute was accepted (partial/full). */
    private BigDecimal creditNoteAmount;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum InvoiceStatus {
        DRAFT, ISSUED, PAID, OVERDUE, CANCELLED, DISPUTED
    }
}
