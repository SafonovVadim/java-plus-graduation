package ru.practicum.service;

import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);
}
