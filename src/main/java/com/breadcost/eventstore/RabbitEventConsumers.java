package com.breadcost.eventstore;

import com.breadcost.config.RabbitMQConfig;
import com.breadcost.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ event consumers — B1: Process events from priority queues.
 * Active only when "rabbit" profile is enabled.
 */
@Component
@Profile("rabbit")
@RequiredArgsConstructor
@Slf4j
public class RabbitEventConsumers {

    private final IdempotencyService idempotencyService;

    /**
     * High-priority queue: order and inventory events.
     * These require immediate processing (e.g., stock updates, order confirmations).
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_HIGH, concurrency = "2-4")
    public void handleHighPriority(DomainEvent event) {
        processEvent(event, "HIGH");
    }

    /**
     * Normal-priority queue: production and notification events.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NORMAL, concurrency = "1-2")
    public void handleNormalPriority(DomainEvent event) {
        processEvent(event, "NORMAL");
    }

    /**
     * Low-priority queue: audit and analytics events.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_LOW, concurrency = "1")
    public void handleLowPriority(DomainEvent event) {
        processEvent(event, "LOW");
    }

    private void processEvent(DomainEvent event, String priority) {
        String key = event.getIdempotencyKey();
        String tenantId = event.getTenantId();
        String eventType = event.getEventType();

        if (key != null && idempotencyService.checkIdempotency(tenantId, eventType, key) != null) {
            log.debug("Skipping duplicate event: type={}, key={}", eventType, key);
            return;
        }

        log.info("Processing {} event: type={}, tenant={}, site={}",
                priority, eventType, tenantId, event.getSiteId());

        if (key != null) {
            idempotencyService.recordExecution(tenantId, eventType, key, "consumed");
        }
    }
}
