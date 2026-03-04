package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A single technology step within a recipe version.
 * Steps are ordered by stepNumber ascending.
 * Each step describes a distinct production phase (e.g. Mixing, Fermentation, Baking).
 */
@Entity
@Table(name = "technology_steps",
       indexes = {
           @Index(name = "idx_ts_tenant_recipe", columnList = "tenant_id, recipe_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnologyStepEntity {

    @Id
    @Column(name = "step_id")
    private String stepId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "recipe_id", nullable = false)
    private String recipeId;

    /** Execution order (1-based). Lower number = earlier in the process. */
    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    /** Short name, e.g. "Mixing", "First fermentation", "Baking" */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Detailed description of what to do during this step */
    @Column(name = "activities", length = 2000)
    private String activities;

    /** Comma-separated list of equipment / instruments used, e.g. "Spiral mixer, Dough hook" */
    @Column(name = "instruments", length = 500)
    private String instruments;

    /** Target duration for this step in minutes */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /** Target temperature in Celsius (optional) */
    @Column(name = "temperature_celsius")
    private Integer temperatureCelsius;

    @Column(name = "created_at_utc", updatable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    @PrePersist
    public void prePersist() {
        if (createdAtUtc == null) createdAtUtc = Instant.now();
        updatedAtUtc = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAtUtc = Instant.now();
    }
}
