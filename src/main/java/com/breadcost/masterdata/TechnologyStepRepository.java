package com.breadcost.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnologyStepRepository extends JpaRepository<TechnologyStepEntity, String> {

    List<TechnologyStepEntity> findByTenantIdAndRecipeIdOrderByStepNumberAsc(
            String tenantId, String recipeId);
}
