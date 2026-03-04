package com.breadcost.masterdata;

import com.breadcost.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final DepartmentRepository departmentRepository;

    // -------------------------------------------------------------------------
    // Request / Response DTOs
    // -------------------------------------------------------------------------

    public record CreateProductRequest(
            String tenantId,
            String departmentId,
            String name,
            String description,
            Product.SaleUnit saleUnit,
            String baseUom,
            BigDecimal price,
            double vatRatePct,
            String createdBy
    ) {}

    public record UpdateProductRequest(
            String name,
            String description,
            Product.SaleUnit saleUnit,
            String baseUom,
            BigDecimal price,
            double vatRatePct,
            Product.ProductStatus status
    ) {}

    // -------------------------------------------------------------------------
    // Operations
    // -------------------------------------------------------------------------

    @Transactional
    public ProductEntity create(CreateProductRequest req) {
        // Validate department exists and belongs to tenant
        departmentRepository.findById(req.departmentId())
                .filter(d -> d.getTenantId().equals(req.tenantId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Department not found or does not belong to tenant: " + req.departmentId()));

        if (productRepository.existsByTenantIdAndNameAndDepartmentId(
                req.tenantId(), req.name(), req.departmentId())) {
            throw new IllegalArgumentException(
                    "Product '" + req.name() + "' already exists in this department");
        }

        ProductEntity entity = ProductEntity.builder()
                .productId(UUID.randomUUID().toString())
                .tenantId(req.tenantId())
                .departmentId(req.departmentId())
                .name(req.name())
                .description(req.description())
                .saleUnit(req.saleUnit())
                .baseUom(req.baseUom())
                .price(req.price())
                .vatRatePct(req.vatRatePct())
                .status(Product.ProductStatus.ACTIVE)
                .createdBy(req.createdBy())
                .build();

        ProductEntity saved = productRepository.save(entity);
        log.info("Product created: id={}, name={}, department={}", saved.getProductId(), saved.getName(), saved.getDepartmentId());
        return saved;
    }

    @Transactional
    public ProductEntity update(String productId, UpdateProductRequest req) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        entity.setName(req.name());
        entity.setDescription(req.description());
        entity.setSaleUnit(req.saleUnit());
        entity.setBaseUom(req.baseUom());
        if (req.price() != null) entity.setPrice(req.price());
        entity.setVatRatePct(req.vatRatePct());
        entity.setStatus(req.status());

        ProductEntity saved = productRepository.save(entity);
        log.info("Product updated: id={}", productId);
        return saved;
    }

    @Transactional
    public ProductEntity setActiveRecipe(String productId, String recipeId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        entity.setActiveRecipeId(recipeId);
        return productRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ProductEntity> listByTenant(String tenantId) {
        return productRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<ProductEntity> listByDepartment(String tenantId, String departmentId) {
        return productRepository.findByTenantIdAndDepartmentId(tenantId, departmentId);
    }

    @Transactional(readOnly = true)
    public ProductEntity getById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }
}
