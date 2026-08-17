package ru.practicum.requests.service;

import ru.practicum.request.EventRequestStatusUpdateRequest;
import ru.practicum.request.EventRequestStatusUpdateResult;
import ru.practicum.request.ParticipationRequestDto;

import java.util.List;

public interface RequestsService {
    EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);
}
