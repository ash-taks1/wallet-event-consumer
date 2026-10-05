package net.alishahidi.vehiclecrossing.walleteventconsumer.repository;

import java.util.List;
import java.util.UUID;

import net.alishahidi.vehiclecrossing.walleteventconsumer.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    boolean existsByEventId(UUID eventId);

    List<ProcessedEventEntity> findByTransactionId(UUID transactionId);
}
