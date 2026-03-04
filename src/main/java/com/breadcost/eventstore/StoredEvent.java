package com.breadcost.eventstore;

import com.breadcost.events.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Stored event wrapper
 * Contains event plus metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredEvent {
    private Long ledgerSeq;
    private DomainEvent event;
    private String eventType;
    private Instant recordedAtUtc;
}
