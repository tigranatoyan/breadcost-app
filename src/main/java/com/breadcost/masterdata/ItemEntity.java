package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemEntity {

    @Id
    private String itemId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false, length = 200)
    private String name;

    /** INGREDIENT, PACKAGING, FG, BYPRODUCT, WIP */
    @Column(nullable = false, length = 50)
    private String type;

    /** Base unit of measure, e.g. KG, PCS, L */
    @Column(nullable = false, length = 20)
    private String baseUom;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Minimum stock threshold — alert when on-hand falls below this */
    @Column(nullable = false)
    @Builder.Default
    private double minStockThreshold = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

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
}
