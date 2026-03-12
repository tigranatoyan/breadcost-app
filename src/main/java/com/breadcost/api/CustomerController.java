package com.breadcost.api;

import com.breadcost.customers.CustomerAddress;
import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerService;
import com.breadcost.security.CustomerSecurityUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

/**
 * Customer Portal API — /v2/customers
 *
 * BC-1101: POST /v2/customers/register → 201 { customerId }
 * BC-1102: POST /v2/customers/login    → 200 { token, customerId, name }
 *          GET  /v2/customers/{id}/profile
 *          PUT  /v2/customers/{id}/profile
 *
 * Public endpoints — no staff authentication required.
 * FR-2.2, FR-2.3 satisfied.
 */
@RestController
@RequestMapping("/v2/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    // ── DTOs ─────────────────────────────────────────────────────────────────

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
        private String password;      // optional on registration
        private String phone;
        private List<AddressRequest> addresses;
    }

    @Data
    public static class LoginRequest {
        @NotBlank private String tenantId;
        @Email @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data
    public static class UpdateProfileRequest {
        private String name;
        private String phone;
        private List<AddressRequest> addresses;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank private String tenantId;
        @Email @NotBlank private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank private String token;
        @NotBlank private String newPassword;
    }

    // ── Registration (BC-1101) ────────────────────────────────────────────────

    /**
     * POST /v2/customers/register → 201 { customerId }
     * 409 if email already registered for this tenant.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterCustomerRequest req) {

        CustomerEntity created = customerService.registerCustomer(
                req.getTenantId(),
                req.getName(),
                req.getEmail(),
                req.getPassword(),
                req.getPhone(),
                mapAddresses(req.getAddresses()));

        log.info("Customer registered: customerId={} tenantId={}", created.getCustomerId(), created.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("customerId", created.getCustomerId()));
    }

    // ── Login (BC-1102) ───────────────────────────────────────────────────────

    /**
     * POST /v2/customers/login → 200 { token, customerId, name }
     * 400 on invalid credentials.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest req) {

        String token = customerService.login(req.getTenantId(), req.getEmail(), req.getPassword());

        CustomerEntity customer = customerService.listCustomers(req.getTenantId())
                .stream()
                .filter(c -> c.getEmail().equals(req.getEmail().trim().toLowerCase()))
                .findFirst()
                .orElseThrow();

        return ResponseEntity.ok(Map.of(
                "token",      token,
                "customerId", customer.getCustomerId(),
                "name",       customer.getName()
        ));
    }

    // ── Profile (BC-1102) ─────────────────────────────────────────────────────

    /**
     * GET /v2/customers/{id}/profile?tenantId=...
     */
    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('Customer','Admin','Manager')")
    public ResponseEntity<CustomerEntity> getProfile(
            @PathVariable("id") String customerId,
            @RequestParam String tenantId) {

        CustomerSecurityUtil.assertOwner(customerId);
        return ResponseEntity.ok(customerService.getProfile(tenantId, customerId));
    }

    /**
     * PUT /v2/customers/{id}/profile?tenantId=...
     */
    @PutMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('Customer','Admin','Manager')")
    public ResponseEntity<CustomerEntity> updateProfile(
            @PathVariable("id") String customerId,
            @RequestParam String tenantId,
            @RequestBody UpdateProfileRequest req) {

        CustomerSecurityUtil.assertOwner(customerId);
        CustomerEntity updated = customerService.updateProfile(
                tenantId, customerId,
                req.getName(), req.getPhone(),
                mapAddresses(req.getAddresses()));

        return ResponseEntity.ok(updated);
    }

    // ── "Me" endpoint (BC-2604) ─────────────────────────────────────────────

    /**
     * GET /v2/customers/me → returns the authenticated customer's profile.
     * Reads customerId from JWT subject.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('Customer')")
    public ResponseEntity<CustomerEntity> me() {
        String customerId = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        String tenantId = CustomerSecurityUtil.getTenantId();
        return ResponseEntity.ok(customerService.getProfile(tenantId, customerId));
    }

    // ── Admin list ────────────────────────────────────────────────────────────

    /** GET /v2/customers?tenantId=... */
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<List<CustomerEntity>> listCustomers(@RequestParam String tenantId) {
        return ResponseEntity.ok(customerService.listCustomers(tenantId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // ── BC-2902: Password Reset ─────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        String token = customerService.forgotPassword(req.getTenantId(), req.getEmail());
        // In production, send token via email/SMS — here we return it for dev/test
        return ResponseEntity.ok(Map.of("resetToken", token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        customerService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successful."));
    }

    // ── Address mapping ─────────────────────────────────────────────────────

    private List<CustomerAddress> mapAddresses(List<AddressRequest> input) {
        if (input == null) return List.of();
        return input.stream()
                .map(a -> CustomerAddress.builder()
                        .label(a.getLabel())
                        .line1(a.getLine1())
                        .line2(a.getLine2())
                        .city(a.getCity())
                        .postalCode(a.getPostalCode())
                        .countryCode(a.getCountryCode())
                        .build())
                .toList();
    }
}
