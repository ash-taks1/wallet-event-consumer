package net.alishahidi.vehiclecrossing.walleteventconsumer.service;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.alishahidi.vehiclecrossing.walleteventconsumer.dto.ProcessedEventDto;
import net.alishahidi.vehiclecrossing.walleteventconsumer.dto.StatsDto;
import net.alishahidi.vehiclecrossing.walleteventconsumer.mapper.ProcessedEventMapper;
import net.alishahidi.vehiclecrossing.walleteventconsumer.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class ConsumerQueryService {

    ProcessedEventRepository processedEvents;
    ProcessedEventMapper processedEventMapper;

    public List<ProcessedEventDto> processedEvents(UUID transactionId) {
        return processedEvents.findByTransactionId(transactionId).stream()
                .map(processedEventMapper::toDto)
                .toList();
    }

    public StatsDto stats() {
        return StatsDto.builder()
                .processedEvents(processedEvents.count())
                .build();
    }
}
