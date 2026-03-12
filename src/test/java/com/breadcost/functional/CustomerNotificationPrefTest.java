package com.breadcost.functional;

import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerRepository;
import com.breadcost.mobile.MobileAppService;
import com.breadcost.mobile.PushNotificationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BC-3004: Customer notification preferences
 *
 * Verifies that push notifications are suppressed when pushEnabled = false.
 */
@DisplayName("R4-S4 :: BC-3004 — Customer Notification Preferences")
class CustomerNotificationPrefTest extends FunctionalTestBase {

    private static final String PREF_TENANT = "pref-tenant-" + UUID.randomUUID();

    @Autowired private CustomerRepository customerRepository;
    @Autowired private MobileAppService mobileAppService;

    private String customerId;

    @BeforeEach
    void seedCustomer() {
        customerId = "pref-cust-" + UUID.randomUUID();
        customerRepository.save(CustomerEntity.builder()
                .customerId(customerId).tenantId(PREF_TENANT)
                .name("Pref Tester").email(customerId + "@test.com")
                .passwordHash("ignored").active(true)
                .addresses(new ArrayList<>())
                .pushEnabled(true).emailEnabled(true).whatsappEnabled(true)
                .build());
    }

    @Test
    @DisplayName("BC-3004 ✓ Push enabled → notification created")
    void pushEnabled_notificationCreated() {
        PushNotificationEntity notif = mobileAppService.sendNotification(
                PREF_TENANT, customerId, "Test", "Body", "ORDER_STATUS", "ord-1");
        assertThat(notif).isNotNull();
        assertThat(notif.getNotificationId()).isNotNull();
    }

    @Test
    @DisplayName("BC-3004 ✓ Push disabled → notification NOT created")
    void pushDisabled_notificationSkipped() {
        // Disable push
        CustomerEntity customer = customerRepository.findById(customerId).orElseThrow();
        customer.setPushEnabled(false);
        customerRepository.save(customer);

        PushNotificationEntity notif = mobileAppService.sendNotification(
                PREF_TENANT, customerId, "Test", "Body", "ORDER_STATUS", "ord-2");
        assertThat(notif).isNull();
    }

    @Test
    @DisplayName("BC-3004 ✓ Notification prefs in profile response")
    void prefsInProfile() throws Exception {
        GET("/v2/customers/" + customerId + "/profile?tenantId=" + PREF_TENANT, bearer("admin1"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.pushEnabled",
                        org.hamcrest.Matchers.is(true)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.whatsappEnabled",
                        org.hamcrest.Matchers.is(true)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.emailEnabled",
                        org.hamcrest.Matchers.is(true)));
    }
}
