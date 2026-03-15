package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recipe_template_steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeTemplateStepEntity {

    @Id
    @Column(name = "step_id")
    private String stepId;

    @Column(name = "template_id", nullable = false)
    private String templateId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String activities;

    @Column(length = 500)
    private String instruments;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "temperature_celsius")
    private Integer temperatureCelsius;
}
