package net.alishahidi.vehiclecrossing.walleteventconsumer.service;

import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.alishahidi.vehiclecrossing.walleteventconsumer.entity.ProcessedEventEntity;
import net.alishahidi.vehiclecrossing.walleteventconsumer.messaging.TransactionEvent;
import net.alishahidi.vehiclecrossing.walleteventconsumer.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionEventProcessor {

    ProcessedEventRepository processedEvents;
    TransactionTemplate transactionTemplate;

    public Outcome process(TransactionEvent event, Instant receivedAt) {
        try {
            return transactionTemplate.execute(status -> applyOnce(event, receivedAt));
        } catch (DataIntegrityViolationException e) {
            logDuplicate(event);
            return Outcome.DUPLICATE;
        }
    }

    private Outcome applyOnce(TransactionEvent event, Instant receivedAt) {
        if (processedEvents.existsByEventId(event.getEventId())) {
            logDuplicate(event);
            return Outcome.DUPLICATE;
        }
        long lagMs = Duration.between(event.getOccurredAt(), receivedAt).toMillis();
        processedEvents.saveAndFlush(ProcessedEventEntity.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .transactionId(event.getData().getTransactionId())
                .transactionType(event.getData().getType())
                .amount(event.getData().getAmount())
                .traceId(event.getTraceId())
                .occurredAt(event.getOccurredAt())
                .receivedAt(receivedAt)
                .lagMs(lagMs)
                .build());
        log.atInfo()
                .setMessage("Event {} processed {} ms after the transaction committed")
                .addArgument(event.getEventType())
                .addArgument(lagMs)
                .addKeyValue("event.action", "event.processed")
                .addKeyValue("event.id", event.getEventId())
                .addKeyValue("event.type", event.getEventType())
                .addKeyValue("transaction.id", event.getData().getTransactionId())
                .addKeyValue("event.lag_ms", lagMs)
                .log();
        return Outcome.PROCESSED;
    }

    private void logDuplicate(TransactionEvent event) {
        log.atWarn()
                .setMessage("Duplicate delivery of event {} skipped; effect already applied")
                .addArgument(event.getEventId())
                .addKeyValue("event.action", "event.duplicate_skipped")
                .addKeyValue("event.id", event.getEventId())
                .addKeyValue("transaction.id", event.getData().getTransactionId())
                .log();
    }

    public enum Outcome {
        PROCESSED,
        DUPLICATE
    }
}
