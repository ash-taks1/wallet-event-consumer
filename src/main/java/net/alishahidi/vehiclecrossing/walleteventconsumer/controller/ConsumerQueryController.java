package net.alishahidi.vehiclecrossing.walleteventconsumer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.alishahidi.vehiclecrossing.walleteventconsumer.dto.ProcessedEventDto;
import net.alishahidi.vehiclecrossing.walleteventconsumer.dto.StatsDto;
import net.alishahidi.vehiclecrossing.walleteventconsumer.service.ConsumerQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Consumer queries", description = "Inspect the events this service has processed")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsumerQueryController {

    ConsumerQueryService queries;

    @Operation(summary = "Processed events of a transaction", description = "Exactly one entry per event, with its commit-to-consumer lag.")
    @GetMapping("/processed-events")
    public List<ProcessedEventDto> processedEvents(@Parameter(description = "Transaction id from wallet-service") @RequestParam UUID transactionId) {
        return queries.processedEvents(transactionId);
    }

    @Operation(summary = "Totals", description = "Number of processed events.")
    @GetMapping("/stats")
    public StatsDto stats() {
        return queries.stats();
    }
}
