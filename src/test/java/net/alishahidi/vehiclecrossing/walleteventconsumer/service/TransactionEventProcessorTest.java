package net.alishahidi.vehiclecrossing.walleteventconsumer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import net.alishahidi.vehiclecrossing.walleteventconsumer.entity.ProcessedEventEntity;
import net.alishahidi.vehiclecrossing.walleteventconsumer.messaging.TransactionEvent;
import net.alishahidi.vehiclecrossing.walleteventconsumer.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class TransactionEventProcessorTest {

    private final ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
    private final TransactionEventProcessor processor = new TransactionEventProcessor(
            repository, new TransactionTemplate(mock(PlatformTransactionManager.class)));

    private final Instant occurredAt = Instant.parse("2026-10-05T10:00:00Z");
    private final TransactionEvent event = TransactionEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType("wallet.withdrawal.completed")
            .occurredAt(occurredAt)
            .traceId("0123456789abcdef0123456789abcdef")
            .data(TransactionEvent.Data.builder().transactionId(UUID.randomUUID()).type("WITHDRAWAL").amount(3_000).build())
            .build();

    @Test
    void newEventIsRecordedWithItsLag() {
        assertThat(processor.process(event, occurredAt.plusMillis(40))).isEqualTo(TransactionEventProcessor.Outcome.PROCESSED);

        ArgumentCaptor<ProcessedEventEntity> saved = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo(event.getEventId());
        assertThat(saved.getValue().getTransactionType()).isEqualTo("WITHDRAWAL");
        assertThat(saved.getValue().getAmount()).isEqualTo(3_000);
        assertThat(saved.getValue().getLagMs()).isEqualTo(40);
    }

    @Test
    void alreadyRecordedEventIsSkipped() {
        when(repository.existsByEventId(event.getEventId())).thenReturn(true);

        assertThat(processor.process(event, Instant.now())).isEqualTo(TransactionEventProcessor.Outcome.DUPLICATE);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void losingAConcurrentInsertCountsAsDuplicate() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_processed_events_event"));

        assertThat(processor.process(event, Instant.now())).isEqualTo(TransactionEventProcessor.Outcome.DUPLICATE);
    }
}
