package com.breadcost.masterdata;

import com.breadcost.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<RecipeEntity, String> {
    List<RecipeEntity> findByTenantIdAndProductId(String tenantId, String productId);
    List<RecipeEntity> findByTenantIdAndProductIdAndStatus(String tenantId, String productId, Recipe.RecipeStatus status);
    Optional<RecipeEntity> findByTenantIdAndProductIdAndVersionNumber(String tenantId, String productId, int versionNumber);

    @Query("SELECT COALESCE(MAX(r.versionNumber), 0) FROM RecipeEntity r WHERE r.tenantId = :tenantId AND r.productId = :productId")
    int findMaxVersionNumber(String tenantId, String productId);
}
