package com.breadcost.multitenancy;

import com.breadcost.customers.CustomerRepository;
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

    private final TenantOnboardingRepository onboardingRepo;
    private final TenantBrandingRepository brandingRepo;
    private final TenantConfigRepository tenantConfigRepo;
    private final TenantSubscriptionRepository subscriptionRepo;
    private final SubscriptionService subscriptionService;

    // Data export dependencies
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepo;

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
        if (!"PENDING".equals(req.getStatus())) {
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
        return onboardingRepo.findByStatus("PENDING");
    }

    public List<TenantOnboardingEntity> getAllRequests() {
        return onboardingRepo.findAll();
    }

    // ── D4.3: Tenant Branding ────────────────────────────────────────────────

    public TenantBrandingEntity getBranding(String tenantId) {
        return brandingRepo.findById(tenantId)
                .orElseGet(() -> TenantBrandingEntity.builder().tenantId(tenantId).build());
    }

    @Transactional
    public TenantBrandingEntity updateBranding(String tenantId, String logoUrl, String primaryColor,
                                                String secondaryColor, String accentColor,
                                                String receiptBusinessName, String receiptFooter,
                                                String receiptHeader, String locale, String timezone) {
        TenantBrandingEntity branding = brandingRepo.findById(tenantId)
                .orElse(TenantBrandingEntity.builder().tenantId(tenantId).build());

        if (logoUrl != null) branding.setLogoUrl(logoUrl);
        if (primaryColor != null) branding.setPrimaryColor(primaryColor);
        if (secondaryColor != null) branding.setSecondaryColor(secondaryColor);
        if (accentColor != null) branding.setAccentColor(accentColor);
        if (receiptBusinessName != null) branding.setReceiptBusinessName(receiptBusinessName);
        if (receiptFooter != null) branding.setReceiptFooter(receiptFooter);
        if (receiptHeader != null) branding.setReceiptHeader(receiptHeader);
        if (locale != null) branding.setLocale(locale);
        if (timezone != null) branding.setTimezone(timezone);

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

        // Tenant config
        tenantConfigRepo.findById(tenantId).ifPresent(c -> export.put("tenantConfig", Map.of(
                "displayName", c.getDisplayName() != null ? c.getDisplayName() : "",
                "mainCurrency", c.getMainCurrency(),
                "orderCutoffTime", c.getOrderCutoffTime()
        )));

        // Branding
        brandingRepo.findById(tenantId).ifPresent(b -> export.put("branding", Map.of(
                "logoUrl", b.getLogoUrl() != null ? b.getLogoUrl() : "",
                "primaryColor", b.getPrimaryColor(),
                "locale", b.getLocale()
        )));

        // Products
        List<ProductEntity> products = productRepo.findByTenantId(tenantId);
        export.put("productsCount", products.size());
        export.put("products", products.stream().map(p -> Map.of(
                "productId", p.getProductId(),
                "name", p.getName(),
                "price", p.getPrice() != null ? p.getPrice().toString() : "0"
        )).toList());

        // Customers
        var customers = customerRepo.findByTenantId(tenantId);
        export.put("customersCount", customers.size());
        export.put("customers", customers.stream().map(c -> Map.of(
                "customerId", c.getCustomerId(),
                "name", c.getName(),
                "email", c.getEmail() != null ? c.getEmail() : ""
        )).toList());

        // Orders
        List<OrderEntity> orders = orderRepo.findByTenantId(tenantId);
        export.put("ordersCount", orders.size());
        export.put("orders", orders.stream().map(o -> Map.of(
                "orderId", o.getOrderId(),
                "status", o.getStatus(),
                "customerName", o.getCustomerName() != null ? o.getCustomerName() : "",
                "totalAmount", o.getTotalAmount() != null ? o.getTotalAmount().toString() : "0",
                "createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : ""
        )).toList());

        // Subscription
        subscriptionRepo.findByTenantId(tenantId).ifPresent(s -> export.put("subscription", Map.of(
                "tier", s.getTierLevel(),
                "active", s.isActive(),
                "expiresAt", s.getExpiryDate() != null ? s.getExpiryDate().toString() : ""
        )));

        log.info("Exported data for tenant={}: {} products, {} customers, {} orders",
                tenantId, products.size(), customers.size(), orders.size());
        return export;
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
        overview.put("pendingOnboarding", onboardingRepo.findByStatus("PENDING").size());

        List<Map<String, Object>> tenantSummaries = new ArrayList<>();
        for (TenantConfigEntity tenant : tenants) {
            String tid = tenant.getTenantId();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("tenantId", tid);
            summary.put("displayName", tenant.getDisplayName() != null ? tenant.getDisplayName() : tid);
            summary.put("currency", tenant.getMainCurrency());
            summary.put("productsCount", productRepo.findByTenantId(tid).size());
            summary.put("ordersCount", orderRepo.findByTenantId(tid).size());
            summary.put("customersCount", customerRepo.findByTenantId(tid).size());

            subscriptionRepo.findByTenantId(tid).ifPresent(s -> {
                summary.put("subscriptionTier", s.getTierLevel());
                summary.put("subscriptionActive", s.isActive());
            });

            tenantSummaries.add(summary);
        }
        overview.put("tenants", tenantSummaries);

        return overview;
    }
}
