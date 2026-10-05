package net.alishahidi.vehiclecrossing.walleteventconsumer.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionEvent {

    UUID eventId;
    String eventType;
    int schemaVersion;
    Instant occurredAt;
    String traceId;
    Data data;

    public TransactionEvent requireValid() {
        if (eventId == null || eventType == null || occurredAt == null || data == null
                || data.getTransactionId() == null || data.getType() == null) {
            throw new IllegalArgumentException("Event is missing required fields: " + eventId);
        }
        return this;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        UUID transactionId;
        String type;
        long amount;
    }
}
