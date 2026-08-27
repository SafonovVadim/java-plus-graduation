package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ParticipationRequestDto createParticipationRequest(@PathVariable Long userId, Long eventId) {
        return requestsService.createParticipationRequest(userId, eventId);
    }

    @Override
    public ParticipationRequestDto cancelParticipationRequest(@PathVariable Long userId, Long requestId) {
        return requestsService.cancelParticipationRequest(userId, requestId);
    }

    @Override
    public List<ParticipationRequestDto> getUserParticipationRequests(@PathVariable Long userId) {
        return requestsService.getUserParticipationRequests(userId);
    }
}
