package ru.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewStats {

    @NotBlank(message = "App name must not be blank")
    private String app;

    @NotBlank(message = "URI must not be blank")
    private String uri;

    private Long hits;
}
