package ru.practicum.service;

public interface PublicRequestsService {
    long countByEventIdAndStatus(long eventId, ru.practicum.events.dto.EventState status);
    boolean existsByEventIdRequesterAndStatus(long eventId, long requesterId, ru.practicum.events.dto.EventState status);
}
