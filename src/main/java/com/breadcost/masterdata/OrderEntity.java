package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    private String orderId;

    @Column
    private Integer orderNumber;

    @Column(nullable = false)
    private String tenantId;

    @Column
    private String siteId;

    @Column
    private String customerId;

    private String customerName;

    private String createdByUserId;

    @Column(nullable = false)
    private String status; // DRAFT, CONFIRMED, IN_PRODUCTION, READY, OUT_FOR_DELIVERY, DELIVERED, CANCELLED

    private Instant requestedDeliveryTime;
    private Instant orderPlacedAt;
    private Instant confirmedAt;

    private boolean rushOrder;

    @Column(precision = 10, scale = 4)
    private BigDecimal rushPremiumPct;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(precision = 18, scale = 4)
    private BigDecimal totalAmount;

    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderLineEntity> lines = new ArrayList<>();

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
}
