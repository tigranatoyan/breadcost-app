package com.breadcost.events;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class OrderCancelledEvent implements DomainEvent {

    private final String orderId;
    private final String tenantId;
    private final String siteId;
    private final String cancelledByUserId;
    private final String reason;
    private final Instant occurredAtUtc;
    private final String idempotencyKey;

    @Override
    public String getEventType() {
        return "ORDER_CANCELLED";
    }
}
