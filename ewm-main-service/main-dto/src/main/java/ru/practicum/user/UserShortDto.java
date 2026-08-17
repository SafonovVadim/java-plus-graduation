package ru.practicum.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO может использоваться для ответов
 */
@Data
@Builder
@AllArgsConstructor
public class UserShortDto {

    private Long id;

    private String name;
}
