package ru.practicum.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.UpdateEventAdminRequest;
import ru.practicum.events.controller.AdminEventsController;
import ru.practicum.events.service.EventsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEventsController.class)
public class AdminEventsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventsService adminEventService;

    @Test
    void getEvents_shouldReturnListOfEvents() throws Exception {
        EventFullDto event = new EventFullDto();
        event.setId(1L);
        event.setTitle("Conference");
        event.setAnnotation("conf");
        event.setEventDate(String.valueOf(LocalDateTime.now().plusDays(7)));
        event.setPaid(true);
        event.setState("PUBLISHED");
        event.setViews(150L);

        when(adminEventService.getEvents(
                eq(List.of(1L, 2L)),
                eq(List.of("PUBLISHED")),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                eq(5)
        )).thenReturn(List.of(event));

        mockMvc.perform(get("/admin/events")
                        .param("users", "1,2")
                        .param("states", "PUBLISHED")
                        .param("from", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Conference"))
                .andExpect(jsonPath("$[0].views").value(150));
    }

    @Test
    void updateEventByAdmin_shouldReturnUpdatedEvent() throws Exception {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(StateAction.PUBLISH_EVENT);

        EventFullDto updatedEvent = new EventFullDto();
        updatedEvent.setId(1L);
        updatedEvent.setTitle("Updated Conference");
        updatedEvent.setState("PUBLISHED");

        when(adminEventService.updateEventByAdmin(1L, request)).thenReturn(updatedEvent);

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Conference"))
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
    }
}
