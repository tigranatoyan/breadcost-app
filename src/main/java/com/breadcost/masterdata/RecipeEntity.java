package com.breadcost.masterdata;

import com.breadcost.domain.Recipe;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for Recipe master data
 * Each row is one version of a recipe for a product
 */
@Entity
@Table(name = "recipes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "product_id", "version_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeEntity {

    @Id
    @Column(name = "recipe_id")
    private String recipeId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recipe.RecipeStatus status;

    @Column(name = "batch_size", nullable = false, precision = 19, scale = 4)
    private BigDecimal batchSize;

    @Column(name = "batch_size_uom", nullable = false)
    private String batchSizeUom;

    @Column(name = "expected_yield", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedYield;

    @Column(name = "yield_uom", nullable = false)
    private String yieldUom;

    @Column(name = "production_notes", length = 2000)
    private String productionNotes;

    /** Lead time from start of production to delivery-ready, in hours */
    @Column(name = "lead_time_hours")
    private Integer leadTimeHours;

    @OneToMany(mappedBy = "recipeId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<RecipeIngredientEntity> ingredients = new ArrayList<>();

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    public void prePersist() {
        createdAtUtc = Instant.now();
        updatedAtUtc = createdAtUtc;
        if (status == null) status = Recipe.RecipeStatus.DRAFT;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAtUtc = Instant.now();
    }
}
