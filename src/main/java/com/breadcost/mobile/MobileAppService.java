package com.breadcost.mobile;

import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Mobile customer app service — BC-2301 (FR-2.1 mobile).
 * Handles device registration and push notification management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileAppService {

    private final MobileDeviceRegistrationRepository deviceRepo;
    private final PushNotificationRepository notificationRepo;
    private final CustomerRepository customerRepository;

    // ── Device Registration ──────────────────────────────────────────────────

    @Transactional
    public MobileDeviceRegistrationEntity registerDevice(String tenantId, String customerId,
                                                          String deviceToken, String platform,
                                                          String deviceName) {
        // Check for existing registration with same token
        Optional<MobileDeviceRegistrationEntity> existing =
                deviceRepo.findByTenantIdAndDeviceToken(tenantId, deviceToken);
        if (existing.isPresent()) {
            MobileDeviceRegistrationEntity reg = existing.get();
            reg.setCustomerId(customerId);
            reg.setActive(true);
            reg.setDeviceName(deviceName);
            return deviceRepo.save(reg);
        }

        MobileDeviceRegistrationEntity reg = MobileDeviceRegistrationEntity.builder()
                .registrationId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .customerId(customerId)
                .deviceToken(deviceToken)
                .platform(platform.toUpperCase())
                .deviceName(deviceName)
                .build();

        log.info("Device registered: customer={} platform={}", customerId, platform);
        return deviceRepo.save(reg);
    }

    @Transactional
    public void unregisterDevice(String tenantId, String registrationId) {
        deviceRepo.findById(registrationId)
                .filter(r -> tenantId.equals(r.getTenantId()))
                .ifPresent(r -> {
                    r.setActive(false);
                    deviceRepo.save(r);
                });
    }

    public List<MobileDeviceRegistrationEntity> getDevices(String tenantId, String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return deviceRepo.findByTenantId(tenantId);
        }
        return deviceRepo.findByTenantIdAndCustomerId(tenantId, customerId);
    }

    public List<MobileDeviceRegistrationEntity> getActiveDevices(String tenantId, String customerId) {
        return deviceRepo.findByTenantIdAndCustomerIdAndActiveTrue(tenantId, customerId);
    }

    // ── Push Notifications ───────────────────────────────────────────────────

    @Transactional
    public PushNotificationEntity sendNotification(String tenantId, String customerId,
                                                    String title, String body,
                                                    String notificationType, String referenceId) {
        // BC-3004: Check customer push notification preference
        Optional<CustomerEntity> customerOpt = customerRepository
                .findByTenantIdAndCustomerId(tenantId, customerId);
        if (customerOpt.isPresent() && !customerOpt.get().isPushEnabled()) {
            log.info("Push notification skipped (disabled): customer={} type={}", customerId, notificationType);
            return null;
        }

        PushNotificationEntity notification = PushNotificationEntity.builder()
                .notificationId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .customerId(customerId)
                .title(title)
                .body(body)
                .notificationType(notificationType)
                .referenceId(referenceId)
                .status("SENT")
                .sentAt(Instant.now())
                .build();

        log.info("Push notification sent: customer={} type={}", customerId, notificationType);
        return notificationRepo.save(notification);
    }

    /**
     * Send order status change notification to all active devices of a customer.
     */
    @Transactional
    public PushNotificationEntity notifyOrderStatusChange(String tenantId, String customerId,
                                                           String orderId, String newStatus) {
        String title = "Order Update";
        String body = String.format("Your order %s is now %s", orderId, newStatus);
        return sendNotification(tenantId, customerId, title, body, "ORDER_STATUS", orderId);
    }

    public List<PushNotificationEntity> getNotifications(String tenantId, String customerId) {
        return notificationRepo.findByTenantIdAndCustomerId(tenantId, customerId);
    }

    public List<PushNotificationEntity> getPendingNotifications(String tenantId, String customerId) {
        return notificationRepo.findByTenantIdAndCustomerIdAndStatus(tenantId, customerId, "PENDING");
    }
}
