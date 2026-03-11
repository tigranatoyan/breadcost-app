package com.breadcost.api;

import com.breadcost.mobile.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Mobile customer app endpoints — BC-2301 (FR-2.1 mobile)
 */
@RestController
@RequestMapping("/v3/mobile")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin','Manager','Cashier','ProductionUser')")
public class MobileAppController {

    private final MobileAppService service;

    // ── Device Registration ──────────────────────────────────────────────────

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public MobileDeviceRegistrationEntity registerDevice(@RequestBody @Valid DeviceRegistrationRequest req) {
        return service.registerDevice(req.tenantId, req.customerId,
                req.deviceToken, req.platform, req.deviceName);
    }

    @DeleteMapping("/devices/{registrationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterDevice(@RequestParam String tenantId,
                                  @PathVariable String registrationId) {
        service.unregisterDevice(tenantId, registrationId);
    }

    @GetMapping("/devices")
    public List<MobileDeviceRegistrationEntity> getDevices(@RequestParam String tenantId,
                                                            @RequestParam String customerId) {
        return service.getDevices(tenantId, customerId);
    }

    @Data
    static class DeviceRegistrationRequest {
        @NotBlank String tenantId;
        @NotBlank String customerId;
        @NotBlank String deviceToken;
        @NotBlank String platform;
        String deviceName;
    }

    // ── Push Notifications ───────────────────────────────────────────────────

    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.CREATED)
    public PushNotificationEntity sendNotification(@RequestBody @Valid NotificationRequest req) {
        return service.sendNotification(req.tenantId, req.customerId,
                req.title, req.body, req.notificationType, req.referenceId);
    }

    @PostMapping("/notifications/order-status")
    @ResponseStatus(HttpStatus.CREATED)
    public PushNotificationEntity notifyOrderStatus(@RequestBody @Valid OrderStatusNotificationRequest req) {
        return service.notifyOrderStatusChange(req.tenantId, req.customerId, req.orderId, req.newStatus);
    }

    @GetMapping("/notifications")
    public List<PushNotificationEntity> getNotifications(@RequestParam String tenantId,
                                                          @RequestParam String customerId) {
        return service.getNotifications(tenantId, customerId);
    }

    @Data
    static class NotificationRequest {
        @NotBlank String tenantId;
        @NotBlank String customerId;
        @NotBlank String title;
        String body;
        @NotBlank String notificationType;
        String referenceId;
    }

    @Data
    static class OrderStatusNotificationRequest {
        @NotBlank String tenantId;
        @NotBlank String customerId;
        @NotBlank String orderId;
        @NotBlank String newStatus;
    }
}
