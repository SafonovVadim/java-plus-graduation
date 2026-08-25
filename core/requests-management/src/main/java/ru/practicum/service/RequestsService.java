package ru.practicum.service;


import ru.practicum.dto.requests.ParticipationRequestDto;

import java.util.List;

public interface RequestsService {

    ParticipationRequestDto createParticipationRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getUserParticipationRequests(Long userId);
}
