package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.feign.PublicRequestClient;
import ru.practicum.service.PublicRequestsService;

@RestController
@RequestMapping("/public/requests")
@RequiredArgsConstructor
public class PublicRequestController implements PublicRequestClient {
    private final PublicRequestsService publicRequestsService;

    @Override
    public Long countByEventIdAndStatus(Long eventId, ru.practicum.events.dto.EventState status) {
        return publicRequestsService.countByEventIdAndStatus(eventId, status);
    }
}
