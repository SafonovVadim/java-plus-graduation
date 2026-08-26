package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.feign.RequestsManagementClient;
import ru.practicum.service.RequestsService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
@Slf4j
public class RequestsController implements RequestsManagementClient {
    private final RequestsService requestsService;

    @Override
    public ParticipationRequestDto createParticipationRequest(Long userId, Long eventId) {
        return requestsService.createParticipationRequest(userId, eventId);
    }

    @Override
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        return requestsService.cancelParticipationRequest(userId, requestId);
    }

    @Override
    public List<ParticipationRequestDto> getUserParticipationRequests(Long userId) {
        return requestsService.getUserParticipationRequests(userId);
    }
}
