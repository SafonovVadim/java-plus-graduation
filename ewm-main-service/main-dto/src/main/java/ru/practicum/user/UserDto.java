package ru.practicum.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * DTO может использоваться для обновления и ответов
 */
@Data
@AllArgsConstructor
public class UserDto {

    @NotNull(message = "Идентификатор не может быть null")
    @Positive(message = "Идентификатор должен быть положительным числом (больше 0)")
    private Long id;

    @NotBlank(message = "Имя не может быть пустым")
    private String name;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Email должен быть в корректном формате")
    private String email;
}
