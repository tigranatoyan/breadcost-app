package com.breadcost.functional;

import com.breadcost.domain.*;
import com.breadcost.invoice.*;
import com.breadcost.masterdata.*;
import com.breadcost.projections.InventoryProjection;
import com.breadcost.purchaseorder.*;
import com.breadcost.subscription.*;
import com.breadcost.supplier.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * R5-S3 tests:
 * G-9  Yield/quality tracking on WO completion (BC-5005)
 * G-4  Invoice dispute workflow (BC-5006)
 * G-5  Subscription expiry enforcement (BC-5007)
 * G-10 Preferred supplier + reverse lookup + auto-PO from plan (BC-5008)
 */
@DisplayName("R5-S3 :: Gap Closure — Yield, Disputes, Expiry, Supplier Mapping")
class GapClosureTest extends FunctionalTestBase {

    @Autowired private ItemRepository itemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private InventoryProjection inventoryProjection;
    @Autowired private ProductionPlanService planService;
    @Autowired private InvoiceService invoiceService;
    @Autowired private SubscriptionService subscriptionService;
    @Autowired private TenantSubscriptionRepository tenantSubRepo;
    @Autowired private PurchaseOrderService poService;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private SupplierCatalogItemRepository catalogItemRepository;
    @Autowired private com.breadcost.customers.CustomerRepository customerRepository;

    private String flourId;
    private String sugarId;
    private String productId;
    private String recipeId;
    private String deptId;

    @BeforeEach
    void seed() {
        deptId = "dept-r5s3-" + UUID.randomUUID().toString().substring(0, 6);
        departmentRepository.save(DepartmentEntity.builder()
                .departmentId(deptId)
                .tenantId(TENANT)
                .name("R5S3 Dept " + deptId)
                .warehouseMode(Department.WarehouseMode.SHARED)
                .status(Department.DepartmentStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());

        flourId = ensureItem("flour-s3-" + deptId, "Flour S3", "INGREDIENT", "KG", 50.0);
        sugarId = ensureItem("sugar-s3-" + deptId, "Sugar S3", "INGREDIENT", "KG", 20.0);

        productId = "prod-s3-" + deptId;
        productRepository.save(ProductEntity.builder()
                .productId(productId)
                .tenantId(TENANT)
                .departmentId(deptId)
                .name("Test Bread S3 " + deptId)
                .saleUnit(Product.SaleUnit.PIECE)
                .baseUom("PC")
                .price(new BigDecimal("8000"))
                .status(Product.ProductStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());

        // Recipe: 5 KG flour + 1 KG sugar → expectedYield = 10 pcs per batch
        recipeId = "recipe-s3-" + deptId;
        recipeRepository.save(RecipeEntity.builder()
                .recipeId(recipeId)
                .tenantId(TENANT)
                .productId(productId)
                .versionNumber(1)
                .status(Recipe.RecipeStatus.ACTIVE)
                .batchSize(new BigDecimal("10"))
                .batchSizeUom("pcs")
                .expectedYield(new BigDecimal("10"))
                .yieldUom("pcs")
                .ingredients(List.of(
                        RecipeIngredientEntity.builder()
                                .ingredientLineId(UUID.randomUUID().toString())
                                .recipeId(recipeId)
                                .itemId(flourId)
                                .itemName("Flour S3")
                                .unitMode(RecipeIngredient.UnitMode.WEIGHT)
                                .recipeQty(new BigDecimal("5.0"))
                                .recipeUom("KG")
                                .purchasingUnitSize(new BigDecimal("1"))
                                .purchasingUom("KG")
                                .wasteFactor(new BigDecimal("0"))
                                .build(),
                        RecipeIngredientEntity.builder()
                                .ingredientLineId(UUID.randomUUID().toString())
                                .recipeId(recipeId)
                                .itemId(sugarId)
                                .itemName("Sugar S3")
                                .unitMode(RecipeIngredient.UnitMode.WEIGHT)
                                .recipeQty(new BigDecimal("1.0"))
                                .recipeUom("KG")
                                .purchasingUnitSize(new BigDecimal("1"))
                                .purchasingUom("KG")
                                .wasteFactor(new BigDecimal("0"))
                                .build()
                ))
                .build());

        // Link active recipe to product
        productRepository.findById(productId).ifPresent(p -> {
            p.setActiveRecipeId(recipeId);
            productRepository.save(p);
        });

        // Seed stock
        seedStock(flourId, new BigDecimal("100"), "KG", new BigDecimal("2.00"));
        seedStock(sugarId, new BigDecimal("20"), "KG", new BigDecimal("5.00"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // G-9: Yield/quality tracking on WO completion (BC-5005)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("G-9 ✓ completeWorkOrder records actualYield + calculates yield variance")
    void completeWorkOrder_withYieldData() {
        // Create plan → generate WOs → approve → start → start WO
        var plan = planService.createPlan(TENANT, "MAIN",
                LocalDate.now(), ProductionPlan.Shift.MORNING, "yield test", "admin1");

        // Manually add a WO to the plan
        var wo = WorkOrderEntity.builder()
                .workOrderId(UUID.randomUUID().toString())
                .plan(plan)
                .tenantId(TENANT)
                .departmentId(deptId)
                .departmentName("R5S3 Dept")
                .productId(productId)
                .productName("Test Bread S3")
                .recipeId(recipeId)
                .targetQty(20)
                .uom("pcs")
                .batchCount(2)
                .status(WorkOrder.Status.PENDING)
                .build();
        plan.getWorkOrders().add(wo);
        plan.setStatus(ProductionPlan.Status.GENERATED);

        // Need to save through repository
        var planRepo = org.springframework.test.util.ReflectionTestUtils.getField(
                planService, "planRepository");
        ((ProductionPlanRepository) planRepo).save(plan);

        planService.approvePlan(TENANT, plan.getPlanId());
        planService.startPlan(TENANT, plan.getPlanId());
        planService.startWorkOrder(TENANT, wo.getWorkOrderId(), "admin1");

        // Complete with yield data: expected 20 (2 batches × 10 yield), actual 18
        WorkOrderEntity completed = planService.completeWorkOrder(
                TENANT, wo.getWorkOrderId(),
                18.0, 2.0, "PASS", "Slight under-yield due to oven temp");

        assertEquals(WorkOrder.Status.COMPLETED, completed.getStatus());
        assertEquals(18.0, completed.getActualYield());
        assertEquals(2.0, completed.getWasteQty());
        assertEquals("PASS", completed.getQualityScore());
        assertNotNull(completed.getQualityNotes());
        // Variance: (18-20)/20 = -10%
        assertNotNull(completed.getYieldVariancePct());
        assertEquals(-10.0, completed.getYieldVariancePct(), 0.1);
    }

    @Test
    @DisplayName("G-9 ✓ completeWorkOrder via REST endpoint with yield body")
    void completeWorkOrder_REST_withYieldBody() throws Exception {
        // Create plan with WO manually
        var plan = planService.createPlan(TENANT, "MAIN",
                LocalDate.now(), ProductionPlan.Shift.MORNING, "rest-yield", "admin1");
        var wo = WorkOrderEntity.builder()
                .workOrderId(UUID.randomUUID().toString())
                .plan(plan)
                .tenantId(TENANT)
                .departmentId(deptId)
                .productId(productId)
                .recipeId(recipeId)
                .targetQty(10)
                .uom("pcs")
                .batchCount(1)
                .status(WorkOrder.Status.PENDING)
                .build();
        plan.getWorkOrders().add(wo);
        plan.setStatus(ProductionPlan.Status.GENERATED);
        var planRepo = (ProductionPlanRepository) org.springframework.test.util.ReflectionTestUtils.getField(
                planService, "planRepository");
        planRepo.save(plan);

        planService.approvePlan(TENANT, plan.getPlanId());
        planService.startPlan(TENANT, plan.getPlanId());
        planService.startWorkOrder(TENANT, wo.getWorkOrderId(), "admin1");

        Map<String, Object> body = Map.of(
                "actualYield", 9.5,
                "wasteQty", 0.5,
                "qualityScore", "PASS",
                "qualityNotes", "Good batch");

        POST("/v1/production-plans/work-orders/" + wo.getWorkOrderId()
                        + "/complete?tenantId=" + TENANT,
                body, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualYield").value(9.5))
                .andExpect(jsonPath("$.qualityScore").value("PASS"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // G-4: Invoice dispute workflow (BC-5006)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("G-4 ✓ disputeInvoice transitions ISSUED → DISPUTED with reason")
    void disputeInvoice_workflow() {
        // Create a customer
        String customerId = "cust-s3-" + UUID.randomUUID().toString().substring(0, 6);
        customerRepository.save(com.breadcost.customers.CustomerEntity.builder()
                .customerId(customerId)
                .tenantId(TENANT)
                .name("Test Customer S3")
                .email(customerId + "@test.com")
                .outstandingBalance(BigDecimal.ZERO)
                .creditLimit(new BigDecimal("100000"))
                .paymentTermsDays(30)
                .build());

        String orderId = "order-dispute-" + UUID.randomUUID().toString().substring(0, 6);
        InvoiceEntity invoice = invoiceService.generateInvoice(
                TENANT, customerId, orderId,
                List.of(Map.of("productId", productId, "productName", "Bread",
                        "qty", "10", "unit", "pcs", "unitPrice", "8000")),
                "GBP");

        invoiceService.issueInvoice(TENANT, invoice.getInvoiceId());

        // Dispute the invoice
        InvoiceEntity disputed = invoiceService.disputeInvoice(
                TENANT, invoice.getInvoiceId(),
                "Wrong quantity delivered", "finance1");

        assertEquals(InvoiceEntity.InvoiceStatus.DISPUTED, disputed.getStatus());
        assertEquals("Wrong quantity delivered", disputed.getDisputeReason());
        assertEquals("finance1", disputed.getDisputedBy());
        assertNotNull(disputed.getDisputedAt());
    }

    @Test
    @DisplayName("G-4 ✓ resolveDispute with credit note adjusts outstanding balance")
    void resolveDispute_withCreditNote() {
        String customerId = "cust-s3-resolve-" + UUID.randomUUID().toString().substring(0, 6);
        customerRepository.save(com.breadcost.customers.CustomerEntity.builder()
                .customerId(customerId)
                .tenantId(TENANT)
                .name("Test Customer Resolve")
                .email(customerId + "@test.com")
                .outstandingBalance(BigDecimal.ZERO)
                .creditLimit(new BigDecimal("500000"))
                .paymentTermsDays(30)
                .build());

        String orderId = "order-resolve-" + UUID.randomUUID().toString().substring(0, 6);
        InvoiceEntity invoice = invoiceService.generateInvoice(
                TENANT, customerId, orderId,
                List.of(Map.of("productId", productId, "productName", "Bread",
                        "qty", "5", "unit", "pcs", "unitPrice", "10000")),
                "GBP");

        invoiceService.issueInvoice(TENANT, invoice.getInvoiceId());
        invoiceService.disputeInvoice(TENANT, invoice.getInvoiceId(),
                "Damaged goods", "finance1");

        // Check outstanding before resolve
        BigDecimal balanceBefore = customerRepository.findById(customerId).get().getOutstandingBalance();

        // Resolve with a £10000 credit note
        BigDecimal creditAmount = new BigDecimal("10000");
        InvoiceEntity resolved = invoiceService.resolveDispute(
                TENANT, invoice.getInvoiceId(),
                "Partial refund for damages", creditAmount);

        // Status should revert (ISSUED or OVERDUE depending on due date)
        assertNotEquals(InvoiceEntity.InvoiceStatus.DISPUTED, resolved.getStatus());
        assertEquals(creditAmount, resolved.getCreditNoteAmount());
        assertNotNull(resolved.getResolvedAt());
        assertEquals("Partial refund for damages", resolved.getResolutionNotes());

        // Outstanding balance should decrease
        BigDecimal balanceAfter = customerRepository.findById(customerId).get().getOutstandingBalance();
        assertEquals(0, balanceAfter.compareTo(balanceBefore.subtract(creditAmount)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // G-5: Subscription expiry enforcement (BC-5007)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("G-5 ✓ getFeatureAccess returns expired=true and empty features for expired sub")
    void expiredSubscription_blocksFeatureAccess() {
        String testTenantId = "tenant-expiry-" + UUID.randomUUID().toString().substring(0, 6);
        subscriptionService.assignTier(testTenantId, "ENTERPRISE", "admin",
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(1));

        Map<String, Object> access = subscriptionService.getFeatureAccess(testTenantId);
        assertEquals(true, access.get("expired"));
        assertTrue(((List<?>) access.get("features")).isEmpty(),
                "Expired subscription should return empty features");
        assertEquals(0, access.get("maxUsers"));
        assertEquals(0, access.get("maxProducts"));
    }

    @Test
    @DisplayName("G-5 ✓ deactivateExpired deactivates past-expiry subscriptions")
    void deactivateExpired_deactivatesExpiredSubs() {
        String testTenantId = "tenant-deact-" + UUID.randomUUID().toString().substring(0, 6);
        TenantSubscriptionEntity sub = subscriptionService.assignTier(
                testTenantId, "STANDARD", "admin",
                LocalDate.now().minusDays(60), LocalDate.now().minusDays(1));

        assertTrue(sub.isActive());

        int count = subscriptionService.deactivateExpired();
        assertTrue(count >= 1, "Should deactivate at least one expired subscription");

        TenantSubscriptionEntity updated = tenantSubRepo.findById(sub.getSubscriptionId()).get();
        assertFalse(updated.isActive());
    }

    @Test
    @DisplayName("G-5 ✓ findExpiringSoon returns subscriptions expiring within N days")
    void findExpiringSoon_returnsUpcomingExpiries() {
        String testTenantId = "tenant-soon-" + UUID.randomUUID().toString().substring(0, 6);
        subscriptionService.assignTier(testTenantId, "BASIC", "admin",
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(3));

        List<TenantSubscriptionEntity> expiring = subscriptionService.findExpiringSoon(7);
        assertTrue(expiring.stream().anyMatch(s -> s.getTenantId().equals(testTenantId)),
                "Should find subscription expiring in 3 days when looking 7 days ahead");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // G-10: Supplier-item mapping + auto-PO (BC-5008)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("G-10 ✓ preferred supplier appears first in reverse lookup")
    void preferredSupplier_rankedFirst() {
        // Create two suppliers
        String s1Id = "sup-cheap-" + UUID.randomUUID().toString().substring(0, 6);
        String s2Id = "sup-pref-" + UUID.randomUUID().toString().substring(0, 6);
        supplierRepository.save(SupplierEntity.builder()
                .supplierId(s1Id).tenantId(TENANT).name("Cheap Supplier").build());
        supplierRepository.save(SupplierEntity.builder()
                .supplierId(s2Id).tenantId(TENANT).name("Preferred Supplier").build());

        // s1 is cheaper but not preferred; s2 is preferred
        catalogItemRepository.save(SupplierCatalogItemEntity.builder()
                .itemId(UUID.randomUUID().toString())
                .supplierId(s1Id).tenantId(TENANT)
                .ingredientId(flourId).ingredientName("Flour")
                .unitPrice(new BigDecimal("1.50")).currency("USD")
                .unit("KG").preferred(false).build());

        catalogItemRepository.save(SupplierCatalogItemEntity.builder()
                .itemId(UUID.randomUUID().toString())
                .supplierId(s2Id).tenantId(TENANT)
                .ingredientId(flourId).ingredientName("Flour")
                .unitPrice(new BigDecimal("2.00")).currency("USD")
                .unit("KG").preferred(true).build());

        List<SupplierCatalogItemEntity> result = poService.findSuppliersForIngredient(TENANT, flourId);
        assertEquals(2, result.size());
        assertTrue(result.get(0).isPreferred(), "Preferred supplier should rank first");
        assertEquals(s2Id, result.get(0).getSupplierId());
    }

    @Test
    @DisplayName("G-10 ✓ auto-generate POs from plan material requirements")
    void generatePOsFromPlan_createsSupplierPOs() {
        // Create supplier with catalog items for flour and sugar
        String supplierId = "sup-auto-" + UUID.randomUUID().toString().substring(0, 6);
        supplierRepository.save(SupplierEntity.builder()
                .supplierId(supplierId).tenantId(TENANT).name("Auto PO Supplier").build());

        catalogItemRepository.save(SupplierCatalogItemEntity.builder()
                .itemId(UUID.randomUUID().toString())
                .supplierId(supplierId).tenantId(TENANT)
                .ingredientId(flourId).ingredientName("Flour")
                .unitPrice(new BigDecimal("2.00")).currency("USD")
                .moq(1.0).unit("KG").preferred(true).build());

        catalogItemRepository.save(SupplierCatalogItemEntity.builder()
                .itemId(UUID.randomUUID().toString())
                .supplierId(supplierId).tenantId(TENANT)
                .ingredientId(sugarId).ingredientName("Sugar")
                .unitPrice(new BigDecimal("5.00")).currency("USD")
                .moq(1.0).unit("KG").preferred(true).build());

        // Create a plan with a WO
        var plan = planService.createPlan(TENANT, "MAIN",
                LocalDate.now(), ProductionPlan.Shift.MORNING, "auto-po test", "admin1");
        var wo = WorkOrderEntity.builder()
                .workOrderId(UUID.randomUUID().toString())
                .plan(plan)
                .tenantId(TENANT)
                .departmentId(deptId)
                .productId(productId)
                .recipeId(recipeId)
                .targetQty(20)
                .uom("pcs")
                .batchCount(2)
                .status(WorkOrder.Status.PENDING)
                .build();
        plan.getWorkOrders().add(wo);
        plan.setStatus(ProductionPlan.Status.GENERATED);
        var planRepo = (ProductionPlanRepository) org.springframework.test.util.ReflectionTestUtils.getField(
                planService, "planRepository");
        planRepo.save(plan);

        // Generate POs from plan
        List<PurchaseOrderEntity> pos = poService.generatePOsFromPlan(TENANT, plan.getPlanId());

        assertFalse(pos.isEmpty(), "Should generate at least one PO");
        assertEquals(supplierId, pos.get(0).getSupplierId());
        assertEquals(PurchaseOrderEntity.PoStatus.DRAFT, pos.get(0).getStatus());
        assertTrue(pos.get(0).getNotes().contains(plan.getPlanId()));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private String ensureItem(String itemId, String name, String type, String uom, double threshold) {
        if (itemRepository.findById(itemId).isEmpty()) {
            ItemEntity item = new ItemEntity();
            item.setItemId(itemId);
            item.setTenantId(TENANT);
            item.setName(name);
            item.setType(type);
            item.setBaseUom(uom);
            item.setMinStockThreshold(threshold);
            item.setActive(true);
            itemRepository.save(item);
        }
        return itemId;
    }

    private void seedStock(String itemId, BigDecimal qty, String uom, BigDecimal unitCost) {
        var event = com.breadcost.events.ReceiveLotEvent.builder()
                .tenantId(TENANT)
                .siteId("MAIN")
                .itemId(itemId)
                .lotId("LOT-" + itemId + "-" + UUID.randomUUID().toString().substring(0, 4))
                .qty(qty)
                .uom(uom)
                .unitCostBase(unitCost)
                .occurredAtUtc(Instant.now())
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        var eventStore = org.springframework.test.util.ReflectionTestUtils.getField(
                inventoryProjection, "eventStore");
        ((com.breadcost.eventstore.EventStore) eventStore)
                .appendEvent(event, com.breadcost.domain.LedgerEntry.EntryClass.FINANCIAL);
    }
}
