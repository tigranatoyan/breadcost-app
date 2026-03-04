package com.breadcost.api;

import com.breadcost.invoice.InvoiceEntity;
import com.breadcost.invoice.InvoiceService;
import com.breadcost.customers.CustomerDiscountRuleEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for BC-E15 Invoicing, Credit Limits and Customer Pricing.
 */
@RestController
@RequestMapping("/v2")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // ── BC-1501: Generate invoice from delivered order ────────────────────────

    /**
     * POST /v2/invoices
     * Body: {tenantId, customerId, orderId, currency, lines:[{productId, productName, qty, unit, unitPrice}]}
     */
    @PostMapping("/invoices")
    public ResponseEntity<Map<String, Object>> generateInvoice(@RequestBody Map<String, Object> body) {
        String tenantId  = (String) body.get("tenantId");
        String customerId = (String) body.get("customerId");
        String orderId   = (String) body.get("orderId");
        String currency  = (String) body.getOrDefault("currency", "GBP");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) body.get("lines");

        InvoiceEntity invoice = invoiceService.generateInvoice(tenantId, customerId, orderId, lines, currency);
        Map<String, Object> resp = new HashMap<>();
        resp.put("invoice", invoice);
        resp.put("lines", invoiceService.getInvoiceLines(invoice.getInvoiceId()));
        return ResponseEntity.status(201).body(resp);
    }

    /**
     * PUT /v2/invoices/{id}/issue
     * Transition DRAFT → ISSUED.
     */
    @PutMapping("/invoices/{id}/issue")
    public ResponseEntity<InvoiceEntity> issueInvoice(@PathVariable String id,
                                                       @RequestParam String tenantId) {
        return ResponseEntity.ok(invoiceService.issueInvoice(tenantId, id));
    }

    /**
     * GET /v2/invoices?tenantId=...
     * List all invoices for tenant.
     */
    @GetMapping("/invoices")
    public List<InvoiceEntity> listInvoices(@RequestParam String tenantId,
                                             @RequestParam(required = false) String customerId) {
        if (customerId != null) {
            return invoiceService.listCustomerInvoices(tenantId, customerId);
        }
        return invoiceService.listInvoices(tenantId);
    }

    /**
     * GET /v2/invoices/{id}?tenantId=...
     * Get invoice with lines.
     */
    @GetMapping("/invoices/{id}")
    public ResponseEntity<Map<String, Object>> getInvoice(@PathVariable String id,
                                                           @RequestParam String tenantId) {
        InvoiceEntity invoice = invoiceService.getInvoice(tenantId, id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("invoice", invoice);
        resp.put("lines", invoiceService.getInvoiceLines(id));
        return ResponseEntity.ok(resp);
    }

    // ── BC-1503: Invoice payment status tracking ──────────────────────────────

    /**
     * PUT /v2/invoices/{id}/pay
     * Body: {tenantId, paidAmount, paidBy}
     */
    @PutMapping("/invoices/{id}/pay")
    public ResponseEntity<InvoiceEntity> markPaid(@PathVariable String id,
                                                   @RequestBody Map<String, Object> body) {
        String tenantId  = (String) body.get("tenantId");
        BigDecimal paidAmount = new BigDecimal(body.get("paidAmount").toString());
        String paidBy = (String) body.getOrDefault("paidBy", "system");
        return ResponseEntity.ok(invoiceService.markPaid(tenantId, id, paidAmount, paidBy));
    }

    /**
     * PUT /v2/invoices/{id}/overdue
     * Manually flag a payment as overdue.
     */
    @PutMapping("/invoices/{id}/overdue")
    public ResponseEntity<InvoiceEntity> markOverdue(@PathVariable String id,
                                                      @RequestParam String tenantId) {
        return ResponseEntity.ok(invoiceService.markOverdue(tenantId, id));
    }

    // ── BC-1502: Payment terms per customer ───────────────────────────────────

    /**
     * PUT /v2/customers/{id}/payment-terms
     * Body: {tenantId, paymentTermsDays}
     */
    @PutMapping("/customers/{id}/payment-terms")
    public ResponseEntity<Object> updatePaymentTerms(@PathVariable String id,
                                                      @RequestBody Map<String, Object> body) {
        String tenantId = (String) body.get("tenantId");
        int days = Integer.parseInt(body.get("paymentTermsDays").toString());
        return ResponseEntity.ok(invoiceService.updatePaymentTerms(tenantId, id, days));
    }

    // ── BC-1504: Credit limit enforcement ────────────────────────────────────

    /**
     * PUT /v2/customers/{id}/credit-limit
     * Body: {tenantId, creditLimit}
     */
    @PutMapping("/customers/{id}/credit-limit")
    public ResponseEntity<Object> updateCreditLimit(@PathVariable String id,
                                                     @RequestBody Map<String, Object> body) {
        String tenantId = (String) body.get("tenantId");
        BigDecimal limit = new BigDecimal(body.get("creditLimit").toString());
        return ResponseEntity.ok(invoiceService.updateCreditLimit(tenantId, id, limit));
    }

    /**
     * GET /v2/customers/{id}/credit-check?tenantId=...&orderAmount=...
     * Returns {allowed: true/false, outstandingBalance, creditLimit}
     */
    @GetMapping("/customers/{id}/credit-check")
    public ResponseEntity<Map<String, Object>> creditCheck(@PathVariable String id,
                                                            @RequestParam String tenantId,
                                                            @RequestParam BigDecimal orderAmount) {
        boolean allowed = invoiceService.hasSufficientCredit(tenantId, id, orderAmount);
        Map<String, Object> resp = new HashMap<>();
        resp.put("allowed", allowed);
        resp.put("customerId", id);
        resp.put("orderAmount", orderAmount);
        return ResponseEntity.ok(resp);
    }

    // ── BC-1505: Customer-specific pricing and discount rules ─────────────────

    /**
     * POST /v2/customers/{id}/discount-rules
     * Body: {tenantId, itemType, itemId, discountPct, fixedPrice, minQty, notes}
     */
    @PostMapping("/customers/{id}/discount-rules")
    public ResponseEntity<CustomerDiscountRuleEntity> addDiscountRule(@PathVariable String id,
                                                                       @RequestBody Map<String, Object> body) {
        String tenantId = (String) body.get("tenantId");
        String itemType = (String) body.getOrDefault("itemType", "PRODUCT");
        String itemId = (String) body.get("itemId");
        BigDecimal discountPct  = body.get("discountPct") != null
                ? new BigDecimal(body.get("discountPct").toString()) : BigDecimal.ZERO;
        BigDecimal fixedPrice = body.get("fixedPrice") != null
                ? new BigDecimal(body.get("fixedPrice").toString()) : null;
        BigDecimal minQty = body.get("minQty") != null
                ? new BigDecimal(body.get("minQty").toString()) : BigDecimal.ONE;
        String notes = (String) body.get("notes");

        CustomerDiscountRuleEntity rule = invoiceService.addDiscountRule(
                tenantId, id, itemType, itemId, discountPct, fixedPrice, minQty, notes);
        return ResponseEntity.status(201).body(rule);
    }

    /**
     * GET /v2/customers/{id}/discount-rules?tenantId=...
     */
    @GetMapping("/customers/{id}/discount-rules")
    public List<CustomerDiscountRuleEntity> listDiscountRules(@PathVariable String id,
                                                               @RequestParam String tenantId) {
        return invoiceService.listDiscountRules(tenantId, id);
    }

    /**
     * DELETE /v2/customers/{customerId}/discount-rules/{ruleId}?tenantId=...
     */
    @DeleteMapping("/customers/{customerId}/discount-rules/{ruleId}")
    public ResponseEntity<Void> deactivateDiscountRule(@PathVariable String customerId,
                                                        @PathVariable String ruleId,
                                                        @RequestParam String tenantId) {
        invoiceService.deactivateDiscountRule(tenantId, ruleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /v2/customers/{id}/effective-price?tenantId=...&itemId=...&basePrice=...&qty=...
     * BC-1505: compute effective price after applying best discount rule.
     */
    @GetMapping("/customers/{id}/effective-price")
    public ResponseEntity<Map<String, Object>> effectivePrice(@PathVariable String id,
                                                               @RequestParam String tenantId,
                                                               @RequestParam String itemId,
                                                               @RequestParam BigDecimal basePrice,
                                                               @RequestParam(defaultValue = "1") BigDecimal qty) {
        BigDecimal effective = invoiceService.computeEffectivePrice(tenantId, id, itemId, basePrice, qty);
        Map<String, Object> resp = new HashMap<>();
        resp.put("customerId", id);
        resp.put("itemId", itemId);
        resp.put("basePrice", basePrice);
        resp.put("effectivePrice", effective);
        return ResponseEntity.ok(resp);
    }
}
