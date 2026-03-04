package com.breadcost.events;

import com.breadcost.domain.OrderLine;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class OrderCreatedEvent implements DomainEvent {

    private final String orderId;
    private final String tenantId;
    private final String siteId;
    private final String customerId;
    private final String customerName;
    private final String createdByUserId;
    private final Instant requestedDeliveryTime;
    private final boolean rushOrder;
    private final BigDecimal rushPremiumPct;
    private final String notes;
    private final List<OrderLine> lines;
    private final Instant occurredAtUtc;
    private final String idempotencyKey;

    @Override
    public String getEventType() {
        return "ORDER_CREATED";
    }
}
