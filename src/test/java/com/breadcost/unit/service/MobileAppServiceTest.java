package com.breadcost.unit.service;

import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerRepository;
import com.breadcost.mobile.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileAppServiceTest {

    @Mock private MobileDeviceRegistrationRepository deviceRepo;
    @Mock private PushNotificationRepository notificationRepo;
    @Mock private CustomerRepository customerRepo;
    @InjectMocks private MobileAppService svc;

    // ── registerDevice ───────────────────────────────────────────────────────

    @Test
    void registerDevice_new_createsRegistration() {
        when(deviceRepo.findByTenantIdAndDeviceToken("t1", "token123")).thenReturn(Optional.empty());
        when(deviceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var reg = svc.registerDevice("t1", "c1", "token123", "ios", "iPhone");

        assertNotNull(reg.getRegistrationId());
        assertEquals("IOS", reg.getPlatform());
        assertEquals("c1", reg.getCustomerId());
    }

    @Test
    void registerDevice_existing_updatesCustomer() {
        var existing = MobileDeviceRegistrationEntity.builder()
                .registrationId("r1").tenantId("t1").deviceToken("token123")
                .customerId("old_c").active(false).build();
        when(deviceRepo.findByTenantIdAndDeviceToken("t1", "token123"))
                .thenReturn(Optional.of(existing));
        when(deviceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var reg = svc.registerDevice("t1", "new_c", "token123", "ios", "iPhone");

        assertEquals("new_c", reg.getCustomerId());
        assertTrue(reg.isActive());
    }

    // ── sendNotification ─────────────────────────────────────────────────────

    @Test
    void sendNotification_pushEnabled_saves() {
        when(customerRepo.findByTenantIdAndCustomerId("t1", "c1"))
                .thenReturn(Optional.of(CustomerEntity.builder().customerId("c1")
                        .pushEnabled(true).build()));
        when(notificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var notif = svc.sendNotification("t1", "c1", "Title", "Body", "ORDER_STATUS", "ord1");

        assertNotNull(notif);
        assertEquals("SENT", notif.getStatus());
        assertEquals("Title", notif.getTitle());
    }

    @Test
    void sendNotification_pushDisabled_returnsNull() {
        when(customerRepo.findByTenantIdAndCustomerId("t1", "c1"))
                .thenReturn(Optional.of(CustomerEntity.builder().customerId("c1")
                        .pushEnabled(false).build()));

        var notif = svc.sendNotification("t1", "c1", "Title", "Body", "ORDER_STATUS", "ord1");

        assertNull(notif);
        verify(notificationRepo, never()).save(any());
    }

    @Test
    void sendNotification_unknownCustomer_sendsAnyway() {
        when(customerRepo.findByTenantIdAndCustomerId("t1", "c1")).thenReturn(Optional.empty());
        when(notificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var notif = svc.sendNotification("t1", "c1", "Title", "Body", "PROMO", null);

        assertNotNull(notif);
    }

    // ── notifyOrderStatusChange ──────────────────────────────────────────────

    @Test
    void notifyOrderStatusChange_sendsWithFormattedBody() {
        when(customerRepo.findByTenantIdAndCustomerId("t1", "c1")).thenReturn(Optional.empty());
        when(notificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var notif = svc.notifyOrderStatusChange("t1", "c1", "ORD-123", "DELIVERED");

        assertEquals("Order Update", notif.getTitle());
        assertTrue(notif.getBody().contains("ORD-123"));
        assertTrue(notif.getBody().contains("DELIVERED"));
    }
}
