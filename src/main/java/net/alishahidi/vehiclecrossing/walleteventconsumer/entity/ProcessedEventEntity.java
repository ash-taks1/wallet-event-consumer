package net.alishahidi.vehiclecrossing.walleteventconsumer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import net.alishahidi.vehiclecrossing.walleteventconsumer.entity.base.BaseEntity;

@Entity
@Table(name = "processed_events",
        // The unique event_id is what makes processing idempotent: a second insert of the same event fails.
        uniqueConstraints = @UniqueConstraint(name = "uq_processed_events_event", columnNames = "event_id"),
        indexes = @Index(name = "ix_processed_events_transaction", columnList = "transaction_id"))
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessedEventEntity extends BaseEntity {

    @Column(name = "event_id", nullable = false, updatable = false)
    UUID eventId;

    @Column(name = "event_type", nullable = false, length = 128)
    String eventType;

    @Column(name = "transaction_id", nullable = false)
    UUID transactionId;

    @Column(name = "transaction_type", nullable = false, length = 32)
    String transactionType;

    @Column(name = "amount", nullable = false)
    long amount;

    @Column(name = "trace_id", length = 64)
    String traceId;

    @Column(name = "occurred_at", nullable = false)
    Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    Instant receivedAt;

    @Column(name = "lag_ms", nullable = false)
    long lagMs;
}
