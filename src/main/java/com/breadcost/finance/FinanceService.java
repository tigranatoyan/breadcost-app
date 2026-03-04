package com.breadcost.finance;

import com.breadcost.invoice.InvoiceEntity;
import com.breadcost.invoice.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Finance service
 * Implements posting rules, PPA, and financial calculations
 */
@Service
@Slf4j
public class FinanceService {

    private final InvoiceRepository invoiceRepo;

    public FinanceService(InvoiceRepository invoiceRepo) {
        this.invoiceRepo = invoiceRepo;
    }

    /**
     * Determine if entry is eligible for FG adjustment
     * Per FG_ADJ_ELIGIBILITY rules
     */
    public boolean isEligibleForFGAdjustment(String eventType, Long ledgerSeq, Long recognitionCutoffSeq) {
        // Entry must be after recognition cutoff
        if (ledgerSeq <= recognitionCutoffSeq) {
            return false;
        }

        // Check event type eligibility
        return switch (eventType) {
            case "IssueToBatch", "ApplyOverhead", "RecordLabor" -> true;
            default -> false;
        };
    }

    /**
     * Calculate unit cost for lot
     */
    public BigDecimal calculateUnitCost(BigDecimal totalCost, BigDecimal qty) {
        if (qty.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(qty, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Apply posting rule for event
     * Per POSTING_RULES.yaml
     */
    public PostingResult applyPostingRule(String eventType) {
        return switch (eventType) {
            case "ReceiveLot" -> PostingResult.builder()
                    .debitAccount("INV_RM")
                    .creditAccount("AP")
                    .build();
            case "IssueToBatch" -> PostingResult.builder()
                    .debitAccount("WIP_MATERIAL")
                    .creditAccount("INV_RM")
                    .build();
            case "RecognizeProduction" -> PostingResult.builder()
                    .debitAccount("INV_FG")
                    .creditAccount("WIP_TOTAL")
                    .build();
            case "TransferInventory" -> PostingResult.builder()
                    .operational(true)
                    .build();
            default -> PostingResult.builder().build();
        };
    }

    public static class PostingResult {
        public String debitAccount;
        public String creditAccount;
        public boolean operational;

        public static PostingResultBuilder builder() {
            return new PostingResultBuilder();
        }

        public static class PostingResultBuilder {
            private String debitAccount;
            private String creditAccount;
            private boolean operational;

            public PostingResultBuilder debitAccount(String debitAccount) {
                this.debitAccount = debitAccount;
                return this;
            }

            public PostingResultBuilder creditAccount(String creditAccount) {
                this.creditAccount = creditAccount;
                return this;
            }

            public PostingResultBuilder operational(boolean operational) {
                this.operational = operational;
                return this;
            }

            public PostingResult build() {
                PostingResult result = new PostingResult();
                result.debitAccount = this.debitAccount;
                result.creditAccount = this.creditAccount;
                result.operational = this.operational;
                return result;
            }
        }
    }

    // ── BC-1603: Financial KPI computation ─────────────────────────────────

    /** Sum of all paid/issued invoice totals within date range. BC-1603 */
    public BigDecimal totalRevenue(String tenantId, LocalDate dateFrom, LocalDate dateTo) {
        List<InvoiceEntity> invoices = invoiceRepo.findByTenantId(tenantId);
        return invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceEntity.InvoiceStatus.PAID
                        || inv.getStatus() == InvoiceEntity.InvoiceStatus.ISSUED)
                .filter(inv -> inv.getIssuedDate() != null
                        && !inv.getIssuedDate().isBefore(dateFrom)
                        && !inv.getIssuedDate().isAfter(dateTo))
                .map(InvoiceEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Placeholder COGS — returns 60% of revenue as a reasonable bakery estimate. BC-1603 */
    public BigDecimal totalCostOfGoods(String tenantId, LocalDate dateFrom, LocalDate dateTo) {
        return totalRevenue(tenantId, dateFrom, dateTo)
                .multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
    }

    /** Gross margin percentage. BC-1603 */
    public BigDecimal grossMarginPct(String tenantId, LocalDate dateFrom, LocalDate dateTo) {
        BigDecimal revenue = totalRevenue(tenantId, dateFrom, dateTo);
        if (revenue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal cogs = totalCostOfGoods(tenantId, dateFrom, dateTo);
        return revenue.subtract(cogs)
                .divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    /** Total number of invoiced orders in range. BC-1603 */
    public long totalOrderCount(String tenantId, LocalDate dateFrom, LocalDate dateTo) {
        return invoiceRepo.findByTenantId(tenantId).stream()
                .filter(inv -> inv.getIssuedDate() != null
                        && !inv.getIssuedDate().isBefore(dateFrom)
                        && !inv.getIssuedDate().isAfter(dateTo))
                .count();
    }

    /** Average order value = revenue / count. BC-1603 */
    public BigDecimal avgOrderValue(String tenantId, LocalDate dateFrom, LocalDate dateTo) {
        long count = totalOrderCount(tenantId, dateFrom, dateTo);
        if (count == 0) return BigDecimal.ZERO;
        return totalRevenue(tenantId, dateFrom, dateTo)
                .divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }

    /** Placeholder delivery completion rate (returns 95.0 if no delivery data). BC-1603 */
    public BigDecimal deliveryCompletionRate(String tenantId, LocalDate dateFrom, LocalDate dateTo) {
        return new BigDecimal("95.00");
    }

    /** Placeholder stock turnover. BC-1603 */
    public BigDecimal stockTurnover(String tenantId, LocalDate dateFrom, LocalDate dateTo) {
        return new BigDecimal("4.50");
    }

    /** Placeholder production efficiency. BC-1603 */
    public BigDecimal productionEfficiency(String tenantId, LocalDate dateFrom, LocalDate dateTo) {
        return new BigDecimal("92.00");
    }

    /** Sum of outstanding (unpaid DRAFT/ISSUED) invoice totals. BC-1603 */
    public BigDecimal outstandingInvoices(String tenantId) {
        return invoiceRepo.findByTenantId(tenantId).stream()
                .filter(inv -> inv.getStatus() == InvoiceEntity.InvoiceStatus.ISSUED
                        || inv.getStatus() == InvoiceEntity.InvoiceStatus.DRAFT)
                .map(InvoiceEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Count of OVERDUE invoices. BC-1603 */
    public long overdueInvoiceCount(String tenantId) {
        return invoiceRepo.findByTenantIdAndStatus(tenantId, InvoiceEntity.InvoiceStatus.OVERDUE)
                .size();
    }
}
