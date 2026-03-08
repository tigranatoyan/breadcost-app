package com.breadcost.invoice;

import com.breadcost.customers.CustomerDiscountRuleEntity;
import com.breadcost.customers.CustomerDiscountRuleRepository;
import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Invoice management service.
 * BC-E15: Invoicing, payment terms, credit limits, and discount rules.
 */
@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepo;
    private final InvoiceLineRepository invoiceLineRepo;
    private final CustomerRepository customerRepo;
    private final CustomerDiscountRuleRepository discountRuleRepo;

    // Incremental invoice counter per tenant (in-memory; production would use DB sequence)
    private static final Map<String, AtomicLong> COUNTERS = new HashMap<>();

    public InvoiceService(InvoiceRepository invoiceRepo,
                          InvoiceLineRepository invoiceLineRepo,
                          CustomerRepository customerRepo,
                          CustomerDiscountRuleRepository discountRuleRepo) {
        this.invoiceRepo = invoiceRepo;
        this.invoiceLineRepo = invoiceLineRepo;
        this.customerRepo = customerRepo;
        this.discountRuleRepo = discountRuleRepo;
    }

    // ── BC-1501: Generate invoice from delivered order ──────────────────────

    /**
     * Generate a DRAFT invoice for a completed order.
     *
     * @param tenantId   tenant context
     * @param customerId customer receiving the invoice
     * @param orderId    source order
     * @param lines      each line: {productId, productName, qty, unit, unitPrice}
     * @param currency   ISO currency code e.g. "GBP"
     * @return the created invoice (DRAFT status)
     */
    public InvoiceEntity generateInvoice(String tenantId, String customerId, String orderId,
                                         List<Map<String, Object>> lines, String currency) {
        // Idempotency: don't create duplicate invoice for same order
        invoiceRepo.findByTenantIdAndOrderId(tenantId, orderId).ifPresent(existing -> {
            throw new IllegalStateException("Invoice already exists for order " + orderId);
        });

        CustomerEntity customer = customerRepo.findById(customerId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

        int paymentTerms = customer.getPaymentTermsDays();
        LocalDate issuedDate = LocalDate.now();
        LocalDate dueDate = issuedDate.plusDays(paymentTerms);

        String invoiceId = UUID.randomUUID().toString();
        String invoiceNumber = generateInvoiceNumber(tenantId, issuedDate);

        // ── BC-1504: credit limit check ──────────────────────────────────────
        List<InvoiceLineEntity> invoiceLines = buildLines(invoiceId, tenantId, customerId, lines);
        BigDecimal subtotal = invoiceLines.stream()
                .map(InvoiceLineEntity::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount);

        BigDecimal creditLimit = customer.getCreditLimit();
        if (creditLimit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newOutstanding = customer.getOutstandingBalance().add(total);
            if (newOutstanding.compareTo(creditLimit) > 0) {
                throw new IllegalStateException(
                        "Credit limit exceeded: limit=" + creditLimit + ", outstanding=" + customer.getOutstandingBalance() + ", new invoice=" + total);
            }
        }

        InvoiceEntity invoice = InvoiceEntity.builder()
                .invoiceId(invoiceId)
                .tenantId(tenantId)
                .customerId(customerId)
                .orderId(orderId)
                .invoiceNumber(invoiceNumber)
                .status(InvoiceEntity.InvoiceStatus.DRAFT)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .totalAmount(total)
                .currencyCode(currency != null ? currency : "GBP")
                .paymentTermsDays(paymentTerms)
                .issuedDate(issuedDate)
                .dueDate(dueDate)
                .build();

        invoiceRepo.save(invoice);
        invoiceLineRepo.saveAll(invoiceLines);

        // Update outstanding balance
        customer.setOutstandingBalance(customer.getOutstandingBalance().add(total));
        customerRepo.save(customer);

        return invoice;
    }

    /**
     * Transition invoice from DRAFT → ISSUED.
     * BC-1501
     */
    public InvoiceEntity issueInvoice(String tenantId, String invoiceId) {
        InvoiceEntity invoice = getById(tenantId, invoiceId);
        if (invoice.getStatus() != InvoiceEntity.InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Can only issue DRAFT invoices, current status: " + invoice.getStatus());
        }
        invoice.setStatus(InvoiceEntity.InvoiceStatus.ISSUED);
        invoice.setIssuedDate(LocalDate.now());
        return invoiceRepo.save(invoice);
    }

    // ── BC-1503: Invoice payment status tracking ──────────────────────────────

    /**
     * Record payment for an invoice, transitioning it to PAID.
     * Also decrements customer outstanding balance.
     *
     * @param tenantId   tenant context
     * @param invoiceId  invoice to pay
     * @param paidAmount amount paid
     * @param paidBy     user/reference making the payment
     */
    public InvoiceEntity markPaid(String tenantId, String invoiceId, BigDecimal paidAmount, String paidBy) {
        InvoiceEntity invoice = getById(tenantId, invoiceId);
        if (invoice.getStatus() == InvoiceEntity.InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice already paid");
        }
        if (invoice.getStatus() == InvoiceEntity.InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot pay a cancelled invoice");
        }

        invoice.setStatus(InvoiceEntity.InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        invoice.setPaidBy(paidBy);
        invoice.setPaidAmount(paidAmount);

        // Decrement outstanding balance
        customerRepo.findById(invoice.getCustomerId()).ifPresent(customer -> {
            BigDecimal newBalance = customer.getOutstandingBalance().subtract(paidAmount)
                    .max(BigDecimal.ZERO);
            customer.setOutstandingBalance(newBalance);
            customerRepo.save(customer);
        });

        return invoiceRepo.save(invoice);
    }

    /**
     * Mark invoice as OVERDUE (for use by a scheduled job or manual trigger).
     * BC-1503
     */
    public InvoiceEntity markOverdue(String tenantId, String invoiceId) {
        InvoiceEntity invoice = getById(tenantId, invoiceId);
        if (invoice.getStatus() != InvoiceEntity.InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only ISSUED invoices can become OVERDUE");
        }
        invoice.setStatus(InvoiceEntity.InvoiceStatus.OVERDUE);
        return invoiceRepo.save(invoice);
    }

    // ── BC-1504: Credit limit enforcement ────────────────────────────────────

    /**
     * Update customer payment terms (days).
     * BC-1502
     */
    public CustomerEntity updatePaymentTerms(String tenantId, String customerId, int paymentTermsDays) {
        CustomerEntity customer = customerRepo.findById(customerId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));
        customer.setPaymentTermsDays(paymentTermsDays);
        return customerRepo.save(customer);
    }

    /**
     * Update customer credit limit.
     * BC-1504
     */
    public CustomerEntity updateCreditLimit(String tenantId, String customerId, BigDecimal creditLimit) {
        CustomerEntity customer = customerRepo.findById(customerId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));
        customer.setCreditLimit(creditLimit);
        return customerRepo.save(customer);
    }

    /**
     * Check if a customer has sufficient credit for a new order.
     * Returns true if allowed, false if credit limit would be exceeded.
     * BC-1504
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientCredit(String tenantId, String customerId, BigDecimal orderAmount) {
        CustomerEntity customer = customerRepo.findById(customerId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElse(null);
        if (customer == null) return false;
        BigDecimal limit = customer.getCreditLimit();
        if (limit.compareTo(BigDecimal.ZERO) == 0) return true; // unlimited
        return customer.getOutstandingBalance().add(orderAmount).compareTo(limit) <= 0;
    }

    // ── BC-1505: Customer-specific pricing and discount rules ─────────────────

    /**
     * Add a discount rule for a customer.
     * BC-1505
     */
    public CustomerDiscountRuleEntity addDiscountRule(String tenantId, String customerId,
                                                       String itemType, String itemId,
                                                       BigDecimal discountPct, BigDecimal fixedPrice,
                                                       BigDecimal minQty, String notes) {
        CustomerDiscountRuleEntity rule = CustomerDiscountRuleEntity.builder()
                .ruleId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .customerId(customerId)
                .itemType(CustomerDiscountRuleEntity.ItemType.valueOf(itemType))
                .itemId(itemId)
                .discountPct(discountPct != null ? discountPct : BigDecimal.ZERO)
                .fixedPrice(fixedPrice)
                .minQty(minQty != null ? minQty : BigDecimal.ONE)
                .active(true)
                .notes(notes)
                .build();
        return discountRuleRepo.save(rule);
    }

    /**
     * List active discount rules for a customer.
     * BC-1505
     */
    @Transactional(readOnly = true)
    public List<CustomerDiscountRuleEntity> listDiscountRules(String tenantId, String customerId) {
        return discountRuleRepo.findByTenantIdAndCustomerIdAndActive(tenantId, customerId, true);
    }

    /**
     * Deactivate a discount rule.
     * BC-1505
     */
    public void deactivateDiscountRule(String tenantId, String ruleId) {
        discountRuleRepo.findById(ruleId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .ifPresent(r -> {
                    r.setActive(false);
                    discountRuleRepo.save(r);
                });
    }

    /**
     * Compute the effective price for a customer+item combination, applying best discount rule.
     * BC-1505
     */
    @Transactional(readOnly = true)
    public BigDecimal computeEffectivePrice(String tenantId, String customerId, String itemId,
                                             BigDecimal basePrice, BigDecimal qty) {
        List<CustomerDiscountRuleEntity> rules =
                discountRuleRepo.findByTenantIdAndCustomerIdAndActive(tenantId, customerId, true);

        // Find best applicable rule
        return rules.stream()
                .filter(r -> r.getMinQty().compareTo(qty) <= 0)
                .filter(r -> r.getItemId() == null || r.getItemId().equals(itemId))
                .reduce(basePrice, (best, rule) -> {
                    BigDecimal candidate;
                    if (rule.getFixedPrice() != null) {
                        candidate = rule.getFixedPrice();
                    } else {
                        BigDecimal multiplier = BigDecimal.ONE.subtract(
                                rule.getDiscountPct().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                        candidate = basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
                    }
                    return candidate.compareTo(best) < 0 ? candidate : best;
                }, (a, b) -> a.compareTo(b) <= 0 ? a : b);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InvoiceEntity> listInvoices(String tenantId) {
        return invoiceRepo.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<InvoiceEntity> listCustomerInvoices(String tenantId, String customerId) {
        return invoiceRepo.findByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Transactional(readOnly = true)
    public InvoiceEntity getInvoice(String tenantId, String invoiceId) {
        return getById(tenantId, invoiceId);
    }

    @Transactional(readOnly = true)
    public List<InvoiceLineEntity> getInvoiceLines(String invoiceId) {
        return invoiceLineRepo.findByInvoiceId(invoiceId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private InvoiceEntity getById(String tenantId, String invoiceId) {
        return invoiceRepo.findByTenantIdAndInvoiceId(tenantId, invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + invoiceId));
    }

    private List<InvoiceLineEntity> buildLines(String invoiceId, String tenantId, String customerId,
                                                List<Map<String, Object>> lines) {
        List<InvoiceLineEntity> result = new ArrayList<>();
        List<CustomerDiscountRuleEntity> rules =
                discountRuleRepo.findByTenantIdAndCustomerIdAndActive(tenantId, customerId, true);

        for (Map<String, Object> l : lines) {
            String productId = (String) l.get("productId");
            String productName = (String) l.getOrDefault("productName", "");
            BigDecimal qty = toBD(l.getOrDefault("qty", "1"));
            String unit = (String) l.getOrDefault("unit", "unit");
            BigDecimal unitPrice = toBD(l.getOrDefault("unitPrice", "0"));

            // Apply best rule for BC-1505 discounts
            BigDecimal discountPct = BigDecimal.ZERO;
            BigDecimal effectivePrice = unitPrice;
            for (CustomerDiscountRuleEntity rule : rules) {
                if (rule.getMinQty().compareTo(qty) <= 0
                        && (rule.getItemId() == null || rule.getItemId().equals(productId))) {
                    if (rule.getFixedPrice() != null) {
                        effectivePrice = rule.getFixedPrice();
                        discountPct = BigDecimal.ZERO;
                    } else if (rule.getDiscountPct().compareTo(BigDecimal.ZERO) > 0) {
                        discountPct = rule.getDiscountPct();
                        BigDecimal multiplier = BigDecimal.ONE.subtract(
                                discountPct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                        effectivePrice = unitPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }

            BigDecimal lineTotal = effectivePrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);

            result.add(InvoiceLineEntity.builder()
                    .lineId(UUID.randomUUID().toString())
                    .invoiceId(invoiceId)
                    .tenantId(tenantId)
                    .productId(productId)
                    .productName(productName)
                    .qty(qty)
                    .unit(unit)
                    .unitPrice(unitPrice)
                    .discountPct(discountPct)
                    .lineTotal(lineTotal)
                    .build());
        }
        return result;
    }

    private BigDecimal toBD(Object val) {
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return new BigDecimal(val.toString());
        return new BigDecimal(val.toString());
    }

    private String generateInvoiceNumber(String tenantId, LocalDate date) {
        long seq = COUNTERS.computeIfAbsent(tenantId, k -> new AtomicLong(0)).incrementAndGet();
        return String.format("INV-%d%02d-%05d", date.getYear(), date.getMonthValue(), seq);
    }
}
