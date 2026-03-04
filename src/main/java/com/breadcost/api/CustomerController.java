package com.breadcost.api;

import com.breadcost.customers.CustomerAddress;
import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Customer Portal API — /v2/customers
 *
 * BC-1101: Customer registration
 *   POST /v2/customers/register → 201 { customerId }
 *   Duplicate email → 409 (handled by GlobalExceptionHandler via IllegalStateException)
 *
 * Accessible without authentication (public, customer self-service).
 * FR-2.2 satisfied.
 */
@RestController
@RequestMapping("/v2/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    // ── Request / Response DTOs ──────────────────────────────────────────────

    @Data
    public static class AddressRequest {
        private String label;
        @NotBlank private String line1;
        private String line2;
        @NotBlank private String city;
        private String postalCode;
        private String countryCode;
    }

    @Data
    public static class RegisterCustomerRequest {
        @NotBlank private String tenantId;
        @NotBlank private String name;
        @Email @NotBlank private String email;
        private String phone;
        private List<AddressRequest> addresses;
    }

    // ── Endpoints ────────────────────────────────────────────────────────────

    /**
     * BC-1101: Register a new customer.
     * POST /v2/customers/register
     *
     * @return 201 Created with { "customerId": "..." }
     *         409 Conflict if email already registered for this tenant
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterCustomerRequest req) {

        List<CustomerAddress> addresses = req.getAddresses() == null ? List.of() :
                req.getAddresses().stream()
                        .map(a -> CustomerAddress.builder()
                                .label(a.getLabel())
                                .line1(a.getLine1())
                                .line2(a.getLine2())
                                .city(a.getCity())
                                .postalCode(a.getPostalCode())
                                .countryCode(a.getCountryCode())
                                .build())
                        .toList();

        CustomerEntity created = customerService.registerCustomer(
                req.getTenantId(),
                req.getName(),
                req.getEmail(),
                req.getPhone(),
                addresses);

        log.info("Customer registered: customerId={} tenantId={}", created.getCustomerId(), created.getTenantId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("customerId", created.getCustomerId()));
    }

    /**
     * List all customers for a tenant (admin use, authenticated endpoint).
     * GET /v2/customers?tenantId=...
     */
    @GetMapping
    public ResponseEntity<List<CustomerEntity>> listCustomers(@RequestParam String tenantId) {
        return ResponseEntity.ok(customerService.listCustomers(tenantId));
    }
}
