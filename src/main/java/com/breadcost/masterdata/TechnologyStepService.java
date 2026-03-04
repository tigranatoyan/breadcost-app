package com.breadcost.masterdata;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TechnologyStepService {

    private final TechnologyStepRepository repository;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    public List<TechnologyStepEntity> listByRecipe(String tenantId, String recipeId) {
        return repository.findByTenantIdAndRecipeIdOrderByStepNumberAsc(tenantId, recipeId);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Transactional
    public TechnologyStepEntity create(String tenantId, String recipeId,
                                       int stepNumber, String name,
                                       String activities, String instruments,
                                       Integer durationMinutes, Integer temperatureCelsius) {
        TechnologyStepEntity entity = TechnologyStepEntity.builder()
                .stepId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .recipeId(recipeId)
                .stepNumber(stepNumber)
                .name(name)
                .activities(activities)
                .instruments(instruments)
                .durationMinutes(durationMinutes)
                .temperatureCelsius(temperatureCelsius)
                .build();
        return repository.save(entity);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Transactional
    public TechnologyStepEntity update(String tenantId, String stepId,
                                       int stepNumber, String name,
                                       String activities, String instruments,
                                       Integer durationMinutes, Integer temperatureCelsius) {
        TechnologyStepEntity entity = repository.findById(stepId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Technology step not found: " + stepId));

        entity.setStepNumber(stepNumber);
        entity.setName(name);
        entity.setActivities(activities);
        entity.setInstruments(instruments);
        entity.setDurationMinutes(durationMinutes);
        entity.setTemperatureCelsius(temperatureCelsius);
        return repository.save(entity);
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(String tenantId, String stepId) {
        TechnologyStepEntity entity = repository.findById(stepId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Technology step not found: " + stepId));
        repository.delete(entity);
    }
}
