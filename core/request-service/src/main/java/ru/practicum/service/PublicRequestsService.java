package ru.practicum.service;

public interface PublicRequestsService {
    long countByEventIdAndStatus(long eventId, ru.practicum.events.dto.EventState status);
}
