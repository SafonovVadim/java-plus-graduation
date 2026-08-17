package ru.practicum.events.dto;

import lombok.Data;
import ru.practicum.categories.CategoryDto;
import ru.practicum.user.UserShortDto;

@Data
public class EventShortDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;
    private String eventDate;
    private UserShortDto initiator;
    private Boolean paid;
    private String title;
    private Long views;
}
