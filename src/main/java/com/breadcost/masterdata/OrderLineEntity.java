package com.breadcost.masterdata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLineEntity {

    @Id
    private String orderLineId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    private String productId;
    private String productName;
    private String departmentId;
    private String departmentName;

    @Column(nullable = false)
    private double qty;

    private String uom;

    @Column(precision = 18, scale = 4)
    private BigDecimal unitPrice;

    private boolean leadTimeConflict;
    private Instant earliestReadyAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
