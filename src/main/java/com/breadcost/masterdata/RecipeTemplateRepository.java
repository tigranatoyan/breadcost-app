package com.breadcost.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeTemplateRepository extends JpaRepository<RecipeTemplateEntity, String> {

    List<RecipeTemplateEntity> findByTenantId(String tenantId);

    List<RecipeTemplateEntity> findByTenantIdAndCategory(String tenantId, String category);
}
