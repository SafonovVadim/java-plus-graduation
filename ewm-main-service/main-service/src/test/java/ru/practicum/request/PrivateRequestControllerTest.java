package ru.practicum.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.error.exception.ForbiddenActionException;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.events.EventState;
import ru.practicum.requests.service.RequestsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.common.Constance.FORMATTER;

@SpringBootTest
@AutoConfigureMockMvc
class PrivateRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequestsService requestsService;

    @Test
    void updateRequestStatus_Success() throws Exception {
        // Given
        Long userId = 1L;
        Long eventId = 2L;

        EventRequestStatusUpdateRequest request = new EventRequestStatusUpdateRequest();
        request.setRequestIds(List.of(1L, 2L));
        request.setStatus(EventState.CONFIRMED);

        ParticipationRequestDto confirmedDto = ParticipationRequestDto.builder()
                .id(1L)
                .created(LocalDateTime.now().format(FORMATTER))
                .event(eventId)
                .requester(3L)
                .status(EventState.CONFIRMED)
                .build();

        EventRequestStatusUpdateResult result = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of(confirmedDto))
                .rejectedRequests(List.of())
                .build();

        when(requestsService.updateRequestStatuses(userId, eventId, request)).thenReturn(result);

        // When & Then
        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.rejectedRequests").isEmpty());
    }

    @Test
    void getEventRequests_Success_WithRequests() throws Exception {
        // Given
        Long userId = 1L;
        Long eventId = 2L;

        ParticipationRequestDto requestDto = ParticipationRequestDto.builder()
                .id(1L)
                .created(LocalDateTime.now().format(FORMATTER))
                .event(eventId)
                .requester(3L)
                .status(EventState.PENDING)
                .build();

        List<ParticipationRequestDto> expectedRequests = List.of(requestDto);

        when(requestsService.getEventRequests(userId, eventId)).thenReturn(expectedRequests);

        // When & Then
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].event").value(eventId.intValue()))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getEventRequests_Success_EmptyList() throws Exception {
        // Given
        Long userId = 1L;
        Long eventId = 2L;

        List<ParticipationRequestDto> emptyRequests = List.of();

        when(requestsService.getEventRequests(userId, eventId)).thenReturn(emptyRequests);

        // When & Then
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getEventRequests_EventNotFound() throws Exception {
        // Given
        Long userId = 1L;
        Long eventId = 999L;

        when(requestsService.getEventRequests(userId, eventId))
                .thenThrow(new NotFoundException("Event with id=" + eventId + " was not found"));


        // When & Then
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Event with id=999 was not found"));
    }

    @Test
    void getEventRequests_Forbidden() throws Exception {
        // Given
        Long userId = 1L;
        Long eventId = 2L;

        when(requestsService.getEventRequests(userId, eventId))
                .thenThrow(new ForbiddenActionException("User is not the initiator of the event"));


        // When & Then
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("User is not the initiator of the event"));
    }

    @Test
    void getEventRequests_InvalidUserId() throws Exception {
        // Given
        String invalidUserId = "abc";
        Long eventId = 2L;

        // When & Then
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", invalidUserId, eventId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEventRequests_InvalidEventId() throws Exception {
        // Given
        Long userId = 1L;
        String invalidEventId = "xyz";

        // When & Then
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, invalidEventId))
                .andExpect(status().isBadRequest());
    }
}
