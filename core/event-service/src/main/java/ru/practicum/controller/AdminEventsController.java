package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<List<EventFullDto>> getEvents(@RequestParam List<Long> users, @RequestParam List<String> states, @RequestParam List<Long> categories, @RequestParam LocalDateTime rangeStart, @RequestParam LocalDateTime rangeEnd, @RequestParam Integer from, @RequestParam Integer size) {
        return ResponseEntity.ok(adminEventsService.getEvents(users, states, categories, rangeStart, rangeEnd, from, size));
    }

    @Override
    public ResponseEntity<EventFullDto> updateEventByAdmin(@PathVariable Long eventId, @RequestBody UpdateEventAdminRequest updateRequest) {
        return ResponseEntity.ok(adminEventsService.updateEventByAdmin(eventId, updateRequest));
    }
}
