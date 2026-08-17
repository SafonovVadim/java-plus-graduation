package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHit {

    private Long id;

    @NotBlank(message = "App name must not be blank")
    private String app;

    @NotBlank(message = "URI must not be blank")
    private String uri;

    @NotBlank(message = "IP must not be blank")
    private String ip;

    @NotNull(message = "Timestamp must not be null")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
