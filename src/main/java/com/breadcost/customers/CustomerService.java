package com.breadcost.customers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Customer management service — BC-E11 Customer Portal.
 *
 * BC-1101: Customer registration
 *   - POST /v2/customers/register
 *   - Unique email per tenant (409 on duplicate)
 *   - FR-2.2 satisfied
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

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
            String phone,
            List<CustomerAddress> addresses) {

        // Duplicate email guard (FR-2.2 — unique email per tenant)
        customerRepository.findByTenantIdAndEmail(tenantId, email.trim().toLowerCase())
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "A customer with email '" + email + "' already exists in this account.");
                });

        CustomerEntity customer = CustomerEntity.builder()
                .customerId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .name(name.trim())
                .email(email.trim().toLowerCase())
                .phone(phone)
                .addresses(addresses != null ? addresses : List.of())
                .active(true)
                .build();

        log.info("Registering customer: tenantId={} email={}", tenantId, customer.getEmail());
        return customerRepository.save(customer);
    }

    public List<CustomerEntity> listCustomers(String tenantId) {
        return customerRepository.findByTenantId(tenantId);
    }
}
