package com.breadcost.api;

import com.breadcost.customers.CustomerDiscountRuleEntity;
import com.breadcost.customers.CustomerDiscountRuleRepository;
import com.breadcost.security.CustomerSecurityUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Customer Discounts", description = "Discount rules and tiered pricing per customer")
@RestController
@RequestMapping("/v2/customers/{customerId}/discounts")
@RequiredArgsConstructor
public class CustomerDiscountController {

    private final CustomerDiscountRuleRepository discountRepo;

    @GetMapping
    @PreAuthorize("hasAnyRole('Customer','Admin','Manager')")
    public ResponseEntity<List<CustomerDiscountRuleEntity>> getDiscounts(
            @PathVariable String customerId,
            @RequestParam String tenantId) {
        CustomerSecurityUtil.assertOwner(customerId);
        List<CustomerDiscountRuleEntity> rules =
                discountRepo.findByTenantIdAndCustomerIdAndActive(tenantId, customerId, true);
        return ResponseEntity.ok(rules);
    }

    @Data
    public static class CreateDiscountRequest {
        @NotBlank private String tenantId;
        private CustomerDiscountRuleEntity.ItemType itemType;
        private String itemId;
        private BigDecimal discountPct;
        private BigDecimal fixedPrice;
        private BigDecimal minQty;
        private String notes;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<CustomerDiscountRuleEntity> createDiscount(
            @PathVariable String customerId,
            @Valid @RequestBody CreateDiscountRequest req) {
        CustomerDiscountRuleEntity rule = CustomerDiscountRuleEntity.builder()
                .ruleId(UUID.randomUUID().toString())
                .tenantId(req.getTenantId())
                .customerId(customerId)
                .itemType(req.getItemType() != null ? req.getItemType() : CustomerDiscountRuleEntity.ItemType.PRODUCT)
                .itemId(req.getItemId())
                .discountPct(req.getDiscountPct() != null ? req.getDiscountPct() : BigDecimal.ZERO)
                .fixedPrice(req.getFixedPrice())
                .minQty(req.getMinQty() != null ? req.getMinQty() : BigDecimal.ONE)
                .active(true)
                .notes(req.getNotes())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(discountRepo.save(rule));
    }

    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<Void> deleteDiscount(
            @PathVariable String customerId,
            @PathVariable String ruleId,
            @RequestParam String tenantId) {
        CustomerDiscountRuleEntity rule = discountRepo.findById(ruleId)
                .filter(r -> r.getTenantId().equals(tenantId) && r.getCustomerId().equals(customerId))
                .orElseThrow(() -> new NoSuchElementException("Discount rule not found: " + ruleId));
        discountRepo.delete(rule);
        return ResponseEntity.noContent().build();
    }
}
