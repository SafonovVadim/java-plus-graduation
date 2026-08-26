package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.UpdateEventAdminRequest;
import ru.practicum.feign.AdminEventsClient;
import ru.practicum.service.AdminEventsService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventsController implements AdminEventsClient {
    private final AdminEventsService adminEventsService;

    @Override
    public ResponseEntity<List<EventFullDto>> getEvents(List<Long> users, List<String> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        return ResponseEntity.ok(adminEventsService.getEvents(users, states, categories, rangeStart, rangeEnd, from, size));
    }

    @Override
    public ResponseEntity<EventFullDto> updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        return ResponseEntity.ok(adminEventsService.updateEventByAdmin(eventId, updateRequest));
    }
}
