package com.breadcost.config;

import com.breadcost.ai.AiSuggestionService;
import com.breadcost.exchangerate.ExchangeRateService;
import com.breadcost.invoice.InvoiceService;
import com.breadcost.masterdata.StockAlertService;
import com.breadcost.masterdata.TenantConfigEntity;
import com.breadcost.masterdata.TenantConfigRepository;
import com.breadcost.subscription.SubscriptionService;
import com.breadcost.subscription.TenantSubscriptionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Centralized scheduled jobs for background processing.
 * D2: Automated operational tasks on cron schedules.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobs {

    private final SubscriptionService subscriptionService;
    private final StockAlertService stockAlertService;
    private final InvoiceService invoiceService;
    private final ExchangeRateService exchangeRateService;
    private final AiSuggestionService aiSuggestionService;
    private final TenantConfigRepository tenantConfigRepo;

    /**
     * D2.1 — Daily at 00:00: Deactivate expired subscriptions.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void deactivateExpiredSubscriptions() {
        log.info("[D2.1] Running: deactivate expired subscriptions");
        int count = subscriptionService.deactivateExpired();
        log.info("[D2.1] Deactivated {} expired subscriptions", count);
    }

    /**
     * D2.2 — Daily at 06:00: Detect low stock and auto-create production plans per tenant.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void lowStockAlertAndAutoPlan() {
        log.info("[D2.2] Running: low stock detection + auto production plans");
        List<TenantConfigEntity> tenants = tenantConfigRepo.findAll();
        for (TenantConfigEntity tenant : tenants) {
            try {
                var alerts = stockAlertService.detectLowStock(tenant.getTenantId());
                if (!alerts.isEmpty()) {
                    log.info("[D2.2] Tenant {}: {} low-stock alerts detected", tenant.getTenantId(), alerts.size());
                    stockAlertService.autoCreateProductionPlan(tenant.getTenantId(), "main", "system");
                }
            } catch (Exception e) {
                log.error("[D2.2] Failed for tenant {}: {}", tenant.getTenantId(), e.getMessage());
            }
        }
    }

    /**
     * D2.3 — Weekly Monday at 08:00: Log subscriptions expiring within 30 days.
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void expiringSubscriptionWarnings() {
        log.info("[D2.3] Running: expiring subscription warnings");
        List<TenantSubscriptionEntity> expiring = subscriptionService.findExpiringSoon(30);
        for (TenantSubscriptionEntity sub : expiring) {
            log.warn("[D2.3] Subscription {} for tenant {} expires on {}",
                    sub.getSubscriptionId(), sub.getTenantId(), sub.getExpiryDate());
        }
        log.info("[D2.3] Found {} subscriptions expiring within 30 days", expiring.size());
    }

    /**
     * D2.4 — Daily at 23:00: Mark overdue invoices across all tenants.
     */
    @Scheduled(cron = "0 0 23 * * *")
    public void markOverdueInvoices() {
        log.info("[D2.4] Running: mark overdue invoices");
        int count = invoiceService.markAllOverdue();
        log.info("[D2.4] Marked {} invoices as overdue", count);
    }

    /**
     * D2.5 — Hourly: Refresh exchange rates from external API for all tenants.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshExchangeRates() {
        log.info("[D2.5] Running: refresh exchange rates");
        try {
            int count = exchangeRateService.refreshAllTenantRates();
            log.info("[D2.5] Refreshed {} exchange rates", count);
        } catch (Exception e) {
            log.error("[D2.5] Exchange rate refresh failed: {}", e.getMessage());
        }
    }

    /**
     * D2.6 — Daily at 02:00: Generate AI replenishment suggestions per tenant.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void generateAiReplenishment() {
        log.info("[D2.6] Running: AI replenishment suggestions");
        List<TenantConfigEntity> tenants = tenantConfigRepo.findAll();
        for (TenantConfigEntity tenant : tenants) {
            try {
                var hints = aiSuggestionService.generateReplenishmentHints(tenant.getTenantId(), "DAILY");
                log.info("[D2.6] Tenant {}: generated {} replenishment hints",
                        tenant.getTenantId(), hints.size());
            } catch (Exception e) {
                log.error("[D2.6] Failed for tenant {}: {}", tenant.getTenantId(), e.getMessage());
            }
        }
    }
}
