package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipe_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeTemplateEntity {

    @Id
    @Column(name = "template_id")
    private String templateId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

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

    @Column(name = "lead_time_hours")
    private Integer leadTimeHours;

    @Column(name = "created_by")
    private String createdBy;

    @OneToMany(mappedBy = "templateId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<RecipeTemplateIngredientEntity> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "templateId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<RecipeTemplateStepEntity> steps = new ArrayList<>();

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    @PrePersist
    public void prePersist() {
        createdAtUtc = Instant.now();
        updatedAtUtc = createdAtUtc;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAtUtc = Instant.now();
    }
}
