package com.breadcost.api;

import com.breadcost.masterdata.RecipeTemplateEntity;
import com.breadcost.masterdata.RecipeTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Recipe Templates", description = "Reusable recipe + technology step blueprints")
@RestController
@RequestMapping("/v1/recipe-templates")
@RequiredArgsConstructor
public class RecipeTemplateController {

    private final RecipeTemplateService templateService;

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record SaveAsTemplateRequest(
            @NotBlank String tenantId,
            @NotBlank String recipeId,
            @NotBlank String name,
            String description,
            String category
    ) {}

    // ── List ──────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Technologist', 'ProductionSupervisor')")
    public ResponseEntity<List<RecipeTemplateEntity>> list(
            @RequestParam String tenantId,
            @RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(templateService.listByCategory(tenantId, category));
        }
        return ResponseEntity.ok(templateService.list(tenantId));
    }

    // ── Get single ────────────────────────────────────────────────────────────

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Technologist', 'ProductionSupervisor')")
    public ResponseEntity<RecipeTemplateEntity> get(
            @PathVariable String templateId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(templateService.getById(tenantId, templateId));
    }

    // ── Save recipe as template ───────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Technologist')")
    public ResponseEntity<RecipeTemplateEntity> saveAsTemplate(
            @Valid @RequestBody SaveAsTemplateRequest req) {
        RecipeTemplateEntity created = templateService.saveRecipeAsTemplate(
                req.tenantId(), req.recipeId(),
                req.name(), req.description(), req.category(),
                getPrincipalName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('Admin', 'Technologist')")
    public ResponseEntity<Void> delete(
            @PathVariable String templateId,
            @RequestParam String tenantId) {
        templateService.delete(tenantId, templateId);
        return ResponseEntity.noContent().build();
    }

    private String getPrincipalName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
