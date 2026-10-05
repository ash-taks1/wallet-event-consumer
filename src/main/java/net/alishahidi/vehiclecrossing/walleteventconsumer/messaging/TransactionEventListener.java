package net.alishahidi.vehiclecrossing.walleteventconsumer.messaging;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.alishahidi.vehiclecrossing.walleteventconsumer.config.RabbitTopology;
import net.alishahidi.vehiclecrossing.walleteventconsumer.service.TransactionEventProcessor;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class TransactionEventListener {

    private static final String TRACE_ID = "traceId";

    JsonMapper jsonMapper;
    TransactionEventProcessor processor;

    @RabbitListener(queues = RabbitTopology.QUEUE)
    void onMessage(Message message) {
        Instant receivedAt = Instant.now();
        TransactionEvent event = parse(message);
        MDC.put(TRACE_ID, event.getTraceId());
        try {
            handle(event, message, receivedAt);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

    private void handle(TransactionEvent event, Message message, Instant receivedAt) {
        log.atInfo()
                .setMessage("Event {} received")
                .addArgument(event.getEventType())
                .addKeyValue("event.action", "event.received")
                .addKeyValue("event.id", event.getEventId())
                .addKeyValue("event.type", event.getEventType())
                .addKeyValue("transaction.id", event.getData().getTransactionId())
                .addKeyValue("messaging.redelivered", message.getMessageProperties().isRedelivered())
                .log();
        processor.process(event, receivedAt);
    }

    private TransactionEvent parse(Message message) {
        try {
            return jsonMapper.readValue(message.getBody(), TransactionEvent.class).requireValid();
        } catch (JacksonException | IllegalArgumentException e) {
            log.atError()
                    .setMessage("Malformed event rejected to the dead-letter queue")
                    .setCause(e)
                    .addKeyValue("event.action", "event.rejected")
                    .addKeyValue("messaging.message_id", message.getMessageProperties().getMessageId())
                    .log();
            throw new AmqpRejectAndDontRequeueException("Malformed event", e);
        }
    }
}
