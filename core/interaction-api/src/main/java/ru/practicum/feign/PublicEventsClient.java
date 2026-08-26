package ru.practicum.feign;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "event-service", path = "/events", configuration = FeignConfig.class)
public interface PublicEventsClient {
    @GetMapping
    ResponseEntity<List<EventShortDto>> getEvents(
            @RequestParam(required = false)
            @Size(max = 1000, message = "Text length must be less than or equal to 1000 characters")
            String text,

            @RequestParam(required = false)
            List<@Positive(message = "Category IDs must be positive numbers") Long> categories,

            @RequestParam(required = false) Boolean paid,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeStart,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeEnd,

            @RequestParam(defaultValue = "false") Boolean onlyAvailable,

            @RequestParam(defaultValue = "EVENT_DATE")
            @Pattern(regexp = "EVENT_DATE|VIEWS", message = "Sort must be either 'EVENT_DATE' or 'VIEWS'")
            String sort,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "From must be greater than or equal to 0")
            Integer from,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size must be greater than 0")
            @Max(value = 1000, message = "Size must be less than or equal to 1000")
            Integer size,

            HttpServletRequest request
    );

    @GetMapping("/{id}")
    ResponseEntity<EventFullDto> getEventByIdFull(
            @PathVariable Long id,
            HttpServletRequest request
    );

    @GetMapping("/{eventId}/short")
    EventShortDto getEventShort(@PathVariable Long eventId);
}
