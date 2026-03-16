package com.breadcost.multitenancy;

import com.breadcost.customers.CustomerRepository;
import com.breadcost.invoice.InvoiceEntity;
import com.breadcost.invoice.InvoiceRepository;
import com.breadcost.masterdata.*;
import com.breadcost.subscription.SubscriptionService;
import com.breadcost.subscription.TenantSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * D4 — Tenant management service handling onboarding, branding, data export, and cross-tenant admin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantManagementService {

    private static final String STATUS_KEY = "status";
    private static final String STATUS_PENDING = "PENDING";
    private static final String CUSTOMERS_COUNT_KEY = "customersCount";
    private static final String PRODUCTS_COUNT_KEY = "productsCount";
    private static final String ORDERS_COUNT_KEY = "ordersCount";

    private final TenantOnboardingRepository onboardingRepo;
    private final TenantBrandingRepository brandingRepo;
    private final TenantConfigRepository tenantConfigRepo;
    private final TenantSubscriptionRepository subscriptionRepo;
    private final SubscriptionService subscriptionService;

    // Data export dependencies
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepo;
    private final InvoiceRepository invoiceRepo;
    private final WorkOrderRepository workOrderRepo;
    private final ProductionPlanRepository productionPlanRepo;

    // ── D4.1: Tenant Onboarding ──────────────────────────────────────────────

    /**
     * Submit a self-service tenant registration request.
     */
    @Transactional
    public TenantOnboardingEntity submitOnboardingRequest(String businessName, String ownerEmail,
                                                           String ownerName, String ownerPhone,
                                                           String country, String currency,
                                                           String requestedTier) {
        // Check for duplicate slug
        String slug = businessName.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
        if (onboardingRepo.findByTenantSlug(slug).isPresent()) {
            throw new IllegalArgumentException("Business name already registered: " + businessName);
        }

        TenantOnboardingEntity request = TenantOnboardingEntity.builder()
                .requestId(UUID.randomUUID().toString())
                .tenantSlug(slug)
                .businessName(businessName)
                .ownerEmail(ownerEmail)
                .ownerName(ownerName)
                .ownerPhone(ownerPhone)
                .country(country)
                .currency(currency != null ? currency : "USD")
                .requestedTier(requestedTier != null ? requestedTier : "BASIC")
                .build();

        log.info("New onboarding request: business={} email={} tier={}", businessName, ownerEmail, requestedTier);
        return onboardingRepo.save(request);
    }

    /**
     * Admin approves an onboarding request → provisions tenant.
     */
    @Transactional
    public TenantOnboardingEntity approveOnboarding(String requestId) {
        TenantOnboardingEntity req = onboardingRepo.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Onboarding request not found: " + requestId));
        if (!STATUS_PENDING.equals(req.getStatus())) {
            throw new IllegalStateException("Request is not pending: " + req.getStatus());
        }

        req.setStatus("APPROVED");
        req.setApprovedAt(Instant.now());

        // Provision: create TenantConfig
        String tenantId = req.getTenantSlug();
        TenantConfigEntity config = TenantConfigEntity.builder()
                .tenantId(tenantId)
                .displayName(req.getBusinessName())
                .mainCurrency(req.getCurrency())
                .build();
        tenantConfigRepo.save(config);

        // Create default branding
        TenantBrandingEntity branding = TenantBrandingEntity.builder()
                .tenantId(tenantId)
                .receiptBusinessName(req.getBusinessName())
                .build();
        brandingRepo.save(branding);

        // Assign subscription tier
        subscriptionService.assignTier(tenantId, req.getRequestedTier().toUpperCase(),
                "SYSTEM", LocalDate.now(), LocalDate.now().plusYears(1));

        req.setProvisionedTenantId(tenantId);
        req.setStatus("PROVISIONED");
        req.setProvisionedAt(Instant.now());

        log.info("Provisioned tenant={} for business={}", tenantId, req.getBusinessName());
        return onboardingRepo.save(req);
    }

    @Transactional
    public TenantOnboardingEntity rejectOnboarding(String requestId, String reason) {
        TenantOnboardingEntity req = onboardingRepo.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Onboarding request not found: " + requestId));
        req.setStatus("REJECTED");
        req.setRejectionReason(reason);
        return onboardingRepo.save(req);
    }

    public List<TenantOnboardingEntity> getPendingRequests() {
        return onboardingRepo.findByStatus(STATUS_PENDING);
    }

    public List<TenantOnboardingEntity> getAllRequests() {
        return onboardingRepo.findAll();
    }

    // ── D4.3: Tenant Branding ────────────────────────────────────────────────

    public TenantBrandingEntity getBranding(String tenantId) {
        return brandingRepo.findById(tenantId)
                .orElseGet(() -> TenantBrandingEntity.builder().tenantId(tenantId).build());
    }

    public record BrandingUpdateRequest(
            String tenantId, String logoUrl, String primaryColor,
            String secondaryColor, String accentColor,
            String receiptBusinessName, String receiptFooter,
            String receiptHeader, String locale, String timezone) {}

    @Transactional
    public TenantBrandingEntity updateBranding(BrandingUpdateRequest req) {
        TenantBrandingEntity branding = brandingRepo.findById(req.tenantId())
                .orElse(TenantBrandingEntity.builder().tenantId(req.tenantId()).build());

        if (req.logoUrl() != null) branding.setLogoUrl(req.logoUrl());
        if (req.primaryColor() != null) branding.setPrimaryColor(req.primaryColor());
        if (req.secondaryColor() != null) branding.setSecondaryColor(req.secondaryColor());
        if (req.accentColor() != null) branding.setAccentColor(req.accentColor());
        if (req.receiptBusinessName() != null) branding.setReceiptBusinessName(req.receiptBusinessName());
        if (req.receiptFooter() != null) branding.setReceiptFooter(req.receiptFooter());
        if (req.receiptHeader() != null) branding.setReceiptHeader(req.receiptHeader());
        if (req.locale() != null) branding.setLocale(req.locale());
        if (req.timezone() != null) branding.setTimezone(req.timezone());

        return brandingRepo.save(branding);
    }

    // ── D4.2: GDPR-Compliant Data Export ─────────────────────────────────────

    /**
     * Export all tenant data as a structured map.
     * In production this would be packaged as a ZIP download.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportTenantData(String tenantId) {
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", Instant.now().toString());
        export.put("tenantId", tenantId);

        addTenantConfigExport(tenantId, export);
        addBrandingExport(tenantId, export);
        addProductsExport(tenantId, export);
        addCustomersExport(tenantId, export);
        addOrdersExport(tenantId, export);
        addSubscriptionExport(tenantId, export);
        addInvoicesExport(tenantId, export);
        addWorkOrdersExport(tenantId, export);
        addProductionPlansExport(tenantId, export);

        log.info("Exported data for tenant={}: {} products, {} customers, {} orders, {} invoices, {} workOrders, {} plans",
                tenantId, export.get(PRODUCTS_COUNT_KEY), export.get(CUSTOMERS_COUNT_KEY),
                export.get(ORDERS_COUNT_KEY), export.get("invoicesCount"),
                export.get("workOrdersCount"), export.get("productionPlansCount"));
        return export;
    }

    private void addTenantConfigExport(String tenantId, Map<String, Object> export) {
        tenantConfigRepo.findById(tenantId).ifPresent(c -> export.put("tenantConfig", Map.of(
                "displayName", c.getDisplayName() != null ? c.getDisplayName() : "",
                "mainCurrency", c.getMainCurrency(),
                "orderCutoffTime", c.getOrderCutoffTime()
        )));
    }

    private void addBrandingExport(String tenantId, Map<String, Object> export) {
        brandingRepo.findById(tenantId).ifPresent(b -> export.put("branding", Map.of(
                "logoUrl", b.getLogoUrl() != null ? b.getLogoUrl() : "",
                "primaryColor", b.getPrimaryColor(),
                "locale", b.getLocale()
        )));
    }

    private void addProductsExport(String tenantId, Map<String, Object> export) {
        List<ProductEntity> products = productRepo.findByTenantId(tenantId);
        export.put(PRODUCTS_COUNT_KEY, products.size());
        export.put("products", products.stream().map(p -> Map.of(
                "productId", p.getProductId(),
                "name", p.getName(),
                "price", p.getPrice() != null ? p.getPrice().toString() : "0"
        )).toList());
    }

    private void addCustomersExport(String tenantId, Map<String, Object> export) {
        var customers = customerRepo.findByTenantId(tenantId);
        export.put(CUSTOMERS_COUNT_KEY, customers.size());
        export.put("customers", customers.stream().map(c -> Map.of(
                "customerId", c.getCustomerId(),
                "name", c.getName(),
                "email", c.getEmail() != null ? c.getEmail() : ""
        )).toList());
    }

    private void addOrdersExport(String tenantId, Map<String, Object> export) {
        List<OrderEntity> orders = orderRepo.findByTenantId(tenantId);
        export.put(ORDERS_COUNT_KEY, orders.size());
        export.put("orders", orders.stream().map(o -> Map.of(
                "orderId", o.getOrderId(),
                STATUS_KEY, o.getStatus(),
                "customerName", o.getCustomerName() != null ? o.getCustomerName() : "",
                "totalAmount", o.getTotalAmount() != null ? o.getTotalAmount().toString() : "0",
                "createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : ""
        )).toList());
    }

    private void addSubscriptionExport(String tenantId, Map<String, Object> export) {
        subscriptionRepo.findByTenantId(tenantId).ifPresent(s -> export.put("subscription", Map.of(
                "tier", s.getTierLevel(),
                "active", s.isActive(),
                "expiresAt", s.getExpiryDate() != null ? s.getExpiryDate().toString() : ""
        )));
    }

    private void addInvoicesExport(String tenantId, Map<String, Object> export) {
        List<InvoiceEntity> invoices = invoiceRepo.findByTenantId(tenantId);
        export.put("invoicesCount", invoices.size());
        export.put("invoices", invoices.stream().map(i -> Map.of(
                "invoiceId", i.getInvoiceId(),
                "invoiceNumber", i.getInvoiceNumber() != null ? i.getInvoiceNumber() : "",
                STATUS_KEY, i.getStatus().name(),
                "totalAmount", i.getTotalAmount() != null ? i.getTotalAmount().toString() : "0",
                "issuedDate", i.getIssuedDate() != null ? i.getIssuedDate().toString() : "",
                "dueDate", i.getDueDate() != null ? i.getDueDate().toString() : ""
        )).toList());
    }

    private void addWorkOrdersExport(String tenantId, Map<String, Object> export) {
        List<WorkOrderEntity> workOrders = workOrderRepo.findByTenantId(tenantId);
        export.put("workOrdersCount", workOrders.size());
        export.put("workOrders", workOrders.stream().map(w -> Map.of(
                "workOrderId", w.getWorkOrderId(),
                "productId", w.getProductId() != null ? w.getProductId() : "",
                STATUS_KEY, w.getStatus() != null ? w.getStatus().name() : "",
                "targetQty", String.valueOf(w.getTargetQty())
        )).toList());
    }

    private void addProductionPlansExport(String tenantId, Map<String, Object> export) {
        List<ProductionPlanEntity> plans = productionPlanRepo.findByTenantId(tenantId);
        export.put("productionPlansCount", plans.size());
        export.put("productionPlans", plans.stream().map(p -> Map.of(
                "planId", p.getPlanId(),
                "planDate", p.getPlanDate() != null ? p.getPlanDate().toString() : "",
                STATUS_KEY, p.getStatus() != null ? p.getStatus().name() : ""
        )).toList());
    }

    // ── D4.4: Cross-Tenant Admin Dashboard ───────────────────────────────────

    /**
     * Get platform-wide overview for super admin.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformOverview() {
        List<TenantConfigEntity> tenants = tenantConfigRepo.findAll();
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalTenants", tenants.size());
        overview.put("pendingOnboarding", onboardingRepo.findByStatus(STATUS_PENDING).size());

        List<Map<String, Object>> tenantSummaries = new ArrayList<>();
        for (TenantConfigEntity tenant : tenants) {
            String tid = tenant.getTenantId();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("tenantId", tid);
            summary.put("displayName", tenant.getDisplayName() != null ? tenant.getDisplayName() : tid);
            summary.put("currency", tenant.getMainCurrency());
            summary.put("suspended", tenant.isSuspended());
            summary.put(PRODUCTS_COUNT_KEY, productRepo.findByTenantId(tid).size());
            summary.put(ORDERS_COUNT_KEY, orderRepo.findByTenantId(tid).size());
            summary.put(CUSTOMERS_COUNT_KEY, customerRepo.findByTenantId(tid).size());

            subscriptionRepo.findByTenantIdAndActive(tid, true).ifPresent(s -> {
                summary.put("subscriptionTier", s.getTierLevel());
                summary.put("subscriptionActive", s.isActive());
            });

            tenantSummaries.add(summary);
        }
        overview.put("tenants", tenantSummaries);

        return overview;
    }

    // ── D4.4: Tenant Suspend / Activate ──────────────────────────────────────

    @Transactional
    public TenantConfigEntity suspendTenant(String tenantId) {
        TenantConfigEntity tenant = tenantConfigRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        tenant.setSuspended(true);
        tenant.setSuspendedAt(Instant.now());
        log.info("Suspended tenant={}", tenantId);
        return tenantConfigRepo.save(tenant);
    }

    @Transactional
    public TenantConfigEntity activateTenant(String tenantId) {
        TenantConfigEntity tenant = tenantConfigRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        tenant.setSuspended(false);
        tenant.setSuspendedAt(null);
        log.info("Activated tenant={}", tenantId);
        return tenantConfigRepo.save(tenant);
    }
}
