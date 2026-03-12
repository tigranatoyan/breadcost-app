package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusHistoryEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String status;

    private String description;

    @Column(nullable = false)
    private long timestampEpochMs;
}
