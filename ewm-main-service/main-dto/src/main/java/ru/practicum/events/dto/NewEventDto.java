package ru.practicum.events.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;
import ru.practicum.events.Location;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {

    @Size(min = 20, max = 2000, message = "Краткое описание должно содержать от 20 до 2000 символов")
    @NotBlank(message = "Краткое описание не может быть пустым или состоять только из пробелов")
    private String annotation;

    @NotNull(message = "ID категории не может быть null")
    private Long category;

    @Size(min = 20, max = 7000, message = "Полное описание должно содержать от 20 до 7000 символов")
    @NotBlank(message = "Полное описание не может быть пустым или состоять только из пробелов")
    private String description;

    @NotNull(message = "Дата и время события не могут быть null")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull(message = "Местоположение не может быть null")
    private Location location;

    @NotNull(message = "Флаг платного участия не может быть null")
    private Boolean paid = false;

    @Min(value = 0, message = "Ограничение участников не может быть отрицательным")
    private Integer participantLimit = 0;

    @NotNull(message = "Флаг модерации заявок не может быть null")
    private Boolean requestModeration = true;

    @Size(min = 3, max = 120, message = "Заголовок должен содержать от 3 до 120 символов")
    @NotBlank(message = "Заголовок не может быть пустым или состоять только из пробелов")
    private String title;
}
