package com.breadcost.api;

import com.breadcost.masterdata.TechnologyStepEntity;
import com.breadcost.masterdata.TechnologyStepService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Technology Steps", description = "Production technology step definitions")
@RestController
@RequestMapping("/v1/technology-steps")
@RequiredArgsConstructor
public class TechnologyStepController {

    private final TechnologyStepService stepService;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin','ProductionUser','FinanceUser','Viewer')")
    public ResponseEntity<List<TechnologyStepEntity>> listSteps(
            @RequestParam String tenantId,
            @RequestParam String recipeId) {
        return ResponseEntity.ok(stepService.listByRecipe(tenantId, recipeId));
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<TechnologyStepEntity> createStep(
            @RequestParam String tenantId,
            @RequestBody StepRequest req) {
        TechnologyStepEntity created = stepService.create(
                tenantId, req.getRecipeId(),
                req.getStepNumber(), req.getName(),
                req.getActivities(), req.getInstruments(),
                req.getDurationMinutes(), req.getTemperatureCelsius());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @PutMapping("/{stepId}")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<TechnologyStepEntity> updateStep(
            @PathVariable String stepId,
            @RequestParam String tenantId,
            @RequestBody StepRequest req) {
        TechnologyStepEntity updated = stepService.update(
                tenantId, stepId,
                req.getStepNumber(), req.getName(),
                req.getActivities(), req.getInstruments(),
                req.getDurationMinutes(), req.getTemperatureCelsius());
        return ResponseEntity.ok(updated);
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{stepId}")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<Void> deleteStep(
            @PathVariable String stepId,
            @RequestParam String tenantId) {
        stepService.delete(tenantId, stepId);
        return ResponseEntity.noContent().build();
    }

    // ─── REQUEST DTO ──────────────────────────────────────────────────────────

    @Data
    public static class StepRequest {
        private String recipeId;
        private int stepNumber;
        private String name;
        private String activities;
        private String instruments;
        private Integer durationMinutes;
        private Integer temperatureCelsius;
    }
}
