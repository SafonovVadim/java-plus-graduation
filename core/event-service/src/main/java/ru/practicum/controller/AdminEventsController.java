package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
    public ResponseEntity<List<EventFullDto>> getEvents(   @RequestParam(required = false) List<Long> users,
                                                           @RequestParam(required = false) List<String> states,
                                                           @RequestParam(required = false) List<Long> categories,
                                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                                           @RequestParam(defaultValue = "0") Integer from,
                                                           @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(adminEventsService.getEvents(users, states, categories, rangeStart, rangeEnd, from, size));
    }

    @Override
    public ResponseEntity<EventFullDto> updateEventByAdmin(@PathVariable Long eventId, @RequestBody UpdateEventAdminRequest updateRequest) {
        return ResponseEntity.ok(adminEventsService.updateEventByAdmin(eventId, updateRequest));
    }
}
