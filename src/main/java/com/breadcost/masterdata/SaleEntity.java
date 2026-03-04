package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pos_sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleEntity {

    @Id
    private String saleId;

    @Column(nullable = false)
    private String tenantId;

    private String siteId;
    private String cashierId;
    private String cashierName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.COMPLETED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(precision = 18, scale = 4)
    private BigDecimal subtotal;

    @Column(precision = 18, scale = 4)
    private BigDecimal totalAmount;

    /** Cash received (if CASH) */
    @Column(precision = 18, scale = 4)
    private BigDecimal cashReceived;

    /** Change given back (if CASH) */
    @Column(precision = 18, scale = 4)
    private BigDecimal changeGiven;

    /** Card terminal reference (if CARD) */
    private String cardReference;

    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<SaleLineEntity> lines = new ArrayList<>();

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

    public enum Status { COMPLETED, REFUNDED, VOID }
    public enum PaymentMethod { CASH, CARD }
}
