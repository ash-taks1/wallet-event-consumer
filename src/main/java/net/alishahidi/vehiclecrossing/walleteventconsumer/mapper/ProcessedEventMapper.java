package net.alishahidi.vehiclecrossing.walleteventconsumer.mapper;

import net.alishahidi.vehiclecrossing.walleteventconsumer.dto.ProcessedEventDto;
import net.alishahidi.vehiclecrossing.walleteventconsumer.entity.ProcessedEventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ProcessedEventMapper {

    @Mapping(target = "processedAt", source = "createdAt")
    ProcessedEventDto toDto(ProcessedEventEntity event);
}
