package ru.practicum.feign;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "request-service", contextId = "participationRequest", path = "/users/{userId}/events/{eventId}/requests", configuration = FeignConfig.class)
public interface ParticipationRequestClient {

    @PatchMapping()
    @ResponseStatus(HttpStatus.OK)
    EventRequestStatusUpdateResult updateRequestStatus(
            @Positive Long userId,
            @Positive Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request);

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    List<ParticipationRequestDto> getEventRequests(@Positive Long userId, @Positive Long eventId);
}
