package ru.practicum.requests;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.EventRequestStatusUpdateRequest;
import ru.practicum.request.EventRequestStatusUpdateResult;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.requests.service.RequestsService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
@Slf4j
public class PrivateRequestController {

    private final RequestsService requestsService;

    @PatchMapping
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult updateRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request) {

        log.info("Получен запрос на изменение статуса заявок для пользователя {} события {}", userId, eventId);
        log.debug("Данные запроса: {}", request);

        EventRequestStatusUpdateResult result = requestsService.updateRequestStatuses(userId, eventId, request);

        log.info("Статус заявок успешно изменён для пользователя {} события {}", userId, eventId);
        log.debug("Результат операции: {}", result);

        return result;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        log.info("Получен запрос на получение заявок для пользователя {} события {}", userId, eventId);

        List<ParticipationRequestDto> requests = requestsService.getEventRequests(userId, eventId);

        log.info("Найдено {} заявок для пользователя {} события {}", requests.size(), userId, eventId);
        return requests;
    }
}
