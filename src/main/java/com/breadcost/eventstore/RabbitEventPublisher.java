package com.breadcost.eventstore;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ-backed event publisher.
 * Publishes domain events to the breadcost.events topic exchange,
 * and also delegates to the in-memory EventStore for local projection replay.
 * Active only when "rabbit" profile is enabled.
 */
@Service
@Profile("rabbit")
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Primary
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final EventStore eventStore;

    @Override
    public Long publish(DomainEvent event, LedgerEntry.EntryClass entryClass) {
        // Store locally for projection engine
        Long ledgerSeq = eventStore.appendEvent(event, entryClass);

        // Derive routing key from event type: e.g. "ReceiveLot" → "inventory.receivelot"
        String routingKey = deriveRoutingKey(event);

        try {
            rabbitTemplate.convertAndSend(routingKey, event);
            log.info("Published event to RabbitMQ: routingKey={}, eventType={}, ledgerSeq={}",
                    routingKey, event.getEventType(), ledgerSeq);
        } catch (Exception e) {
            log.error("Failed to publish event to RabbitMQ: eventType={}, ledgerSeq={}, error={}",
                    event.getEventType(), ledgerSeq, e.getMessage());
            // Event is still stored locally — MQ publish failure doesn't lose data
        }

        return ledgerSeq;
    }

    private String deriveRoutingKey(DomainEvent event) {
        String type = event.getEventType().toLowerCase();
        if (type.contains("order")) return "order." + type;
        if (type.contains("inventory") || type.contains("receive") || type.contains("issue")
                || type.contains("transfer") || type.contains("backflush")) return "inventory." + type;
        if (type.contains("production") || type.contains("batch")) return "production." + type;
        return "audit." + type;
    }
}
