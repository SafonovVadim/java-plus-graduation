package ru.practicum.handler;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@SpringBootApplication(scanBasePackages = "ru.practicum")
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ApiErrorResponse(
                "BAD_REQUEST",
                "Incorrectly made request.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }
}

