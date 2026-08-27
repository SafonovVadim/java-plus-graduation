package ru.practicum.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO может использоваться для ответов
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserShortDto {

    private Long id;

    private String name;
}
