package com.breadcost.unit.service;

import com.breadcost.masterdata.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnologyStepServiceTest {

    @Mock private TechnologyStepRepository repo;
    @InjectMocks private TechnologyStepService svc;

    @Test
    void create_savesStep() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var step = svc.create("t1", "r1", 1, "Mixing",
                "Mix ingredients", "Industrial mixer", 30, null);

        assertNotNull(step.getStepId());
        assertEquals("t1", step.getTenantId());
        assertEquals("r1", step.getRecipeId());
        assertEquals(1, step.getStepNumber());
        assertEquals("Mixing", step.getName());
        assertEquals(30, step.getDurationMinutes());
    }

    @Test
    void update_setsFields() {
        var existing = TechnologyStepEntity.builder()
                .stepId("s1").tenantId("t1").recipeId("r1").stepNumber(1).name("Old").build();
        when(repo.findById("s1")).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.update("t1", "s1", 2, "Baking",
                "Bake in oven", "Oven", 45, 180);

        assertEquals(2, result.getStepNumber());
        assertEquals("Baking", result.getName());
        assertEquals(45, result.getDurationMinutes());
        assertEquals(180, result.getTemperatureCelsius());
    }

    @Test
    void update_wrongTenant_throws() {
        var existing = TechnologyStepEntity.builder().stepId("s1").tenantId("t2").build();
        when(repo.findById("s1")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> svc.update("t1", "s1", 1, "X", null, null, null, null));
    }

    @Test
    void delete_success() {
        var existing = TechnologyStepEntity.builder().stepId("s1").tenantId("t1").build();
        when(repo.findById("s1")).thenReturn(Optional.of(existing));

        svc.delete("t1", "s1");

        verify(repo).delete(existing);
    }

    @Test
    void delete_wrongTenant_throws() {
        var existing = TechnologyStepEntity.builder().stepId("s1").tenantId("t2").build();
        when(repo.findById("s1")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> svc.delete("t1", "s1"));
    }
}
