package net.alishahidi.vehiclecrossing.walleteventconsumer.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessedEventDto {
    UUID eventId;
    String eventType;
    UUID transactionId;
    String transactionType;
    long amount;
    String traceId;
    Instant occurredAt;
    Instant receivedAt;
    Instant processedAt;
    long lagMs;
}
