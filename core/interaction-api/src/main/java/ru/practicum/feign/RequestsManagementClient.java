package ru.practicum.feign;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.requests.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "request-service", path = "/users/{userId}/requests", configuration = FeignConfig.class)

public interface RequestsManagementClient {

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    ParticipationRequestDto createParticipationRequest(Long userId, @RequestParam @Positive Long eventId);

    @PatchMapping("/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    ParticipationRequestDto cancelParticipationRequest(Long userId, @PathVariable Long requestId);

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    List<ParticipationRequestDto> getUserParticipationRequests(Long userId);

}
