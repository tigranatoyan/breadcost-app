package com.breadcost.customers;

import com.breadcost.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Customer management service — BC-E11 Customer Portal.
 *
 * BC-1101: Customer registration
 * BC-1102: Customer login and profile management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ── BC-1101: Registration ─────────────────────────────────────────────────

    /**
     * Registers a new customer.
     *
     * @throws IllegalStateException if a customer with the same email already
     *         exists in the tenant (→ 409 via GlobalExceptionHandler)
     */
    @Transactional
    public CustomerEntity registerCustomer(
            String tenantId,
            String name,
            String email,
            String rawPassword,
            String phone,
            List<CustomerAddress> addresses) {

        String normalizedEmail = email.trim().toLowerCase();

        // Duplicate email guard (FR-2.2 — unique email per tenant)
        customerRepository.findByTenantIdAndEmail(tenantId, normalizedEmail)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "A customer with email '" + email + "' already exists in this account.");
                });

        String hash = (rawPassword != null && !rawPassword.isBlank())
                ? passwordEncoder.encode(rawPassword) : null;

        CustomerEntity customer = CustomerEntity.builder()
                .customerId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .name(name.trim())
                .email(normalizedEmail)
                .passwordHash(hash)
                .phone(phone)
                .addresses(addresses != null ? addresses : List.of())
                .active(true)
                .build();

        log.info("Registering customer: tenantId={} email={}", tenantId, normalizedEmail);
        return customerRepository.save(customer);
    }

    // ── BC-1102: Login ────────────────────────────────────────────────────────

    /**
     * Authenticates a customer and returns a JWT token.
     *
     * @return JWT string scoped to the customer (role=Customer)
     * @throws IllegalArgumentException on invalid credentials → 400/401
     */
    public String login(String tenantId, String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        CustomerEntity customer = customerRepository
                .findByTenantIdAndEmail(tenantId, normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials."));

        if (!customer.isActive()) {
            throw new IllegalArgumentException("Account is inactive.");
        }
        if (customer.getPasswordHash() == null
                || !passwordEncoder.matches(rawPassword, customer.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials.");
        }

        log.info("Customer login ok: tenantId={} customerId={}", tenantId, customer.getCustomerId());
        return jwtUtil.generate(customer.getCustomerId(), List.of("Customer"), tenantId);
    }

    // ── BC-1102: Profile ──────────────────────────────────────────────────────

    /**
     * Returns a customer profile.
     *
     * @throws IllegalArgumentException if the customer is not found
     */
    public CustomerEntity getProfile(String tenantId, String customerId) {
        return customerRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }

    /**
     * Updates mutable profile fields: name, phone, addresses.
     *
     * @throws IllegalArgumentException if the customer is not found
     */
    @Transactional
    public CustomerEntity updateProfile(
            String tenantId,
            String customerId,
            String name,
            String phone,
            List<CustomerAddress> addresses) {

        CustomerEntity customer = getProfile(tenantId, customerId);

        if (name != null && !name.isBlank())   customer.setName(name.trim());
        if (phone != null)                     customer.setPhone(phone.trim());
        if (addresses != null) {
            customer.getAddresses().clear();
            customer.getAddresses().addAll(addresses);
        }

        log.info("Profile updated: tenantId={} customerId={}", tenantId, customerId);
        return customerRepository.save(customer);
    }

    // ── utilities ─────────────────────────────────────────────────────────────

    public List<CustomerEntity> listCustomers(String tenantId) {
        return customerRepository.findByTenantId(tenantId);
    }

    // ── BC-2902: Password Reset ────────────────────────────────────────────

    /**
     * Generates a password reset token for the customer with the given email.
     * Token expires in 1 hour. Returns the token string (caller sends to customer).
     *
     * @throws NoSuchElementException if no customer found for tenant+email
     */
    @Transactional
    public String forgotPassword(String tenantId, String email) {
        String normalizedEmail = email.trim().toLowerCase();
        CustomerEntity customer = customerRepository.findByTenantIdAndEmail(tenantId, normalizedEmail)
                .orElseThrow(() -> new NoSuchElementException("Customer not found."));

        String tokenValue = UUID.randomUUID().toString();
        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .id(UUID.randomUUID().toString())
                .customerId(customer.getCustomerId())
                .token(tokenValue)
                .expiresAtEpochMs(System.currentTimeMillis() + 3_600_000) // 1 hour
                .used(false)
                .build();
        resetTokenRepository.save(token);

        log.info("Password reset token generated: tenantId={} customerId={}",
                tenantId, customer.getCustomerId());
        return tokenValue;
    }

    /**
     * Resets the customer's password using a valid, unused, non-expired token.
     *
     * @throws NoSuchElementException if token not found or already used (404)
     * @throws IllegalArgumentException if token expired (400)
     */
    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetTokenEntity token = resetTokenRepository.findByToken(tokenValue)
                .filter(t -> !t.isUsed())
                .orElseThrow(() -> new NoSuchElementException("Invalid or already used reset token."));

        if (token.getExpiresAtEpochMs() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("Token expired.");
        }

        CustomerEntity customer = customerRepository.findById(token.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found."));

        customer.setPasswordHash(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);

        token.setUsed(true);
        resetTokenRepository.save(token);

        log.info("Password reset completed: customerId={}", customer.getCustomerId());
    }
}
