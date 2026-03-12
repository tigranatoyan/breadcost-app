package com.breadcost.functional;

import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-2905: Individual address CRUD
 *
 * POST   /v2/customers/{id}/addresses
 * PUT    /v2/customers/{id}/addresses/{index}
 * DELETE /v2/customers/{id}/addresses/{index}
 */
@DisplayName("R4-S4 :: BC-2905 — Customer Address CRUD")
class CustomerAddressTest extends FunctionalTestBase {

    private static final String ADDR_TENANT = "addr-tenant-" + UUID.randomUUID();

    @Autowired private CustomerRepository customerRepository;

    private String customerId;

    @BeforeEach
    void seedCustomer() {
        customerId = "addr-cust-" + UUID.randomUUID();
        customerRepository.save(CustomerEntity.builder()
                .customerId(customerId).tenantId(ADDR_TENANT)
                .name("Address Tester").email(customerId + "@test.com")
                .passwordHash("ignored").active(true)
                .addresses(new ArrayList<>())
                .build());
    }

    @Test
    @DisplayName("BC-2905 ✓ Add address → returned in list")
    void addAddress() throws Exception {
        Map<String, String> addr = Map.of("label", "Office", "line1", "123 Main St", "city", "Yerevan", "countryCode", "AM");
        POST("/v2/customers/" + customerId + "/addresses?tenantId=" + ADDR_TENANT, addr, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].label", is("Office")))
                .andExpect(jsonPath("$[0].line1", is("123 Main St")));
    }

    @Test
    @DisplayName("BC-2905 ✓ Update address by index")
    void updateAddress() throws Exception {
        // Add first
        Map<String, String> addr = Map.of("label", "Home", "line1", "Old St", "city", "Yerevan", "countryCode", "AM");
        POST("/v2/customers/" + customerId + "/addresses?tenantId=" + ADDR_TENANT, addr, bearer("admin1"))
                .andExpect(status().isCreated());
        // Update
        Map<String, String> updated = Map.of("label", "Home", "line1", "New St", "city", "Gyumri", "countryCode", "AM");
        PUT("/v2/customers/" + customerId + "/addresses/0?tenantId=" + ADDR_TENANT, updated, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].line1", is("New St")))
                .andExpect(jsonPath("$[0].city", is("Gyumri")));
    }

    @Test
    @DisplayName("BC-2905 ✓ Delete address by index")
    void deleteAddress() throws Exception {
        // Add two
        POST("/v2/customers/" + customerId + "/addresses?tenantId=" + ADDR_TENANT,
                Map.of("label", "A", "line1", "1st", "city", "Y", "countryCode", "AM"), bearer("admin1"));
        POST("/v2/customers/" + customerId + "/addresses?tenantId=" + ADDR_TENANT,
                Map.of("label", "B", "line1", "2nd", "city", "Y", "countryCode", "AM"), bearer("admin1"));
        // Delete first
        DELETE("/v2/customers/" + customerId + "/addresses/0?tenantId=" + ADDR_TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].label", is("B")));
    }

    @Test
    @DisplayName("BC-2905 ✓ Max 5 addresses enforced")
    void maxAddresses() throws Exception {
        for (int i = 0; i < 5; i++) {
            POST("/v2/customers/" + customerId + "/addresses?tenantId=" + ADDR_TENANT,
                    Map.of("label", "Addr" + i, "line1", "St " + i, "city", "C", "countryCode", "AM"),
                    bearer("admin1"))
                    .andExpect(status().isCreated());
        }
        // 6th should fail
        POST("/v2/customers/" + customerId + "/addresses?tenantId=" + ADDR_TENANT,
                Map.of("label", "Addr5", "line1", "St 5", "city", "C", "countryCode", "AM"),
                bearer("admin1"))
                .andExpect(status().isBadRequest());
    }
}
