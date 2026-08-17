package ru.practicum.events;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.StatsClient;
import ru.practicum.dto.EndpointHit;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.EventShortDto;

import ru.practicum.events.controller.PublicEventsController;
import ru.practicum.events.service.EventsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.common.Constance.FORMATTER;

@WebMvcTest(PublicEventsController.class)
public class PublicEventsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventsService eventService;

    @MockBean
    private StatsClient statsClient;

    @Test
    void getEvents_shouldReturnEventsAndRecordHit() throws Exception {
        EventShortDto event1 = new EventShortDto();
        event1.setId(1L);
        event1.setTitle("Festival");
        event1.setAnnotation("Summer music festival");
        event1.setConfirmedRequests(5L);
        event1.setEventDate(LocalDateTime.now().format(FORMATTER));
        event1.setPaid(true);
        event1.setViews(100L);

        when(eventService.getPublishedEvents(
                eq("music"),
                isNull(),
                eq(true),
                isNull(),
                isNull(),
                eq(false),
                eq(true),
                eq(0),
                eq(10)
        )).thenReturn(List.of(event1));

        mockMvc.perform(get("/events")
                        .param("text", "music")
                        .param("paid", "true")
                        .param("sort", "VIEWS")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Festival"))
                .andExpect(jsonPath("$[0].views").value(100));

        verify(statsClient).hit(any(EndpointHit.class));
    }

    @Test
    void getEventById_shouldReturnEventAndRecordHit() throws Exception {
        EventFullDto event = new EventFullDto();
        event.setId(1L);
        event.setTitle("Concert");
        event.setDescription("Rock concert");
        event.setEventDate(LocalDateTime.now().plusDays(5).format(FORMATTER));
        event.setPaid(false);
        event.setViews(200L);

        when(eventService.getPublishedEventById(1L)).thenReturn(event);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Concert"))
                .andExpect(jsonPath("$.views").value(200));

        verify(statsClient).hit(any(EndpointHit.class));
    }
}

