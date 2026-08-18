package ru.practicum.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.coyote.BadRequestException;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.error.exception.ConflictException;
import ru.practicum.error.exception.EventCreationRuleException;
import ru.practicum.error.exception.ForbiddenActionException;
import ru.practicum.error.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@SpringBootApplication(scanBasePackages = "ru.practicum")
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка ошибок валидации (400 Bad Request)
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<String> errorMessages = fieldErrors.stream()
                .map(fe -> String.format("Field: %s. Error: %s. Value: %s",
                        fe.getField(),
                        fe.getDefaultMessage(),
                        (fe.getRejectedValue() != null) ? fe.getRejectedValue().toString() : "null"))
                .collect(Collectors.toList());

        return new ErrorResponse(
                "BAD_REQUEST",
                "Incorrectly made request.",
                String.join("; ", errorMessages),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errorMessages = ex.getConstraintViolations().stream()
                .map(violation -> String.format(
                        ("Constraint: %s. Path: %s. Invalid value: %s. Message: %s"),
                        violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        violation.getPropertyPath().toString(),
                        violation.getInvalidValue(),
                        violation.getMessage()
                ))
                .collect(Collectors.toList());

        return new ErrorResponse(
                "BAD_REQUEST",
                "Incorrectly made request.",
                String.join("; ", errorMessages),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(EventCreationRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleEventCreationRule(EventCreationRuleException ex) {
        String errorMessage = String.format(
                ("Field: %s. Error: должно содержать дату, которая ещё не наступила. Value: %s"),
                ex.getField(),
                (ex.getRejectedValue() != null) ? ex.getRejectedValue().toString() : "null"
        );

        return new ErrorResponse(
                "FORBIDDEN",
                "For the requested operation the conditions are not met.",
                errorMessage,
                LocalDateTime.now()
        );
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenAction(
            ForbiddenActionException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                "FORBIDDEN",
                "For the requested operation the conditions are not met.",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT); // 409 Conflict
    }

    /**
     * Обработка конфликтов дубликат email
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootCauseMessage = Objects.requireNonNull(ex.getRootCause()).getMessage();

        return new ErrorResponse(
                "CONFLICT",
                "Integrity constraint has been violated.",
                rootCauseMessage,
                //"constraint [uq_email]",
                //до конца не ясно какой сценарий,
                // пока пусть будет фиксированное значение при любом конфликте в БД
                LocalDateTime.now()
        );
    }

    /**
     * Обработка кастомных конфликтов (например: категория используется)
     */
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictException.class)
    public ErrorResponse handleConflictException(ConflictException ex) {
        return new ErrorResponse(
                "CONFLICT",
                "Conflict occurred.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ErrorResponse handleNotFoundException(NotFoundException ex) {
        return new ErrorResponse(
                "NOT_FOUND",
                "The required object was not found.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    /**
     * Общая обработка любых других исключений (500 Internal Server Error)
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleAllUncaughtException(Exception ex) {
        return new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Internal server error.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String value = ex.getValue() != null ? ex.getValue().toString() : "null";

        return new ErrorResponse(
                "BAD_REQUEST",
                "Incorrectly made request.",
                String.format("Parameter '%s' with value '%s' cannot be converted to required type", paramName, value),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            HandlerMethodValidationException ex) {
        Map<String, String> errors = new HashMap<>();

        // Получаем все результаты валидации
        for (ParameterValidationResult validationResult : ex.getAllValidationResults()) {
            // Получаем ошибки для текущего аргумента
            for (Object error : validationResult.getResolvableErrors()) {
                if (error instanceof FieldError fieldError) {
                    String fieldName = fieldError.getField();
                    String errorMessage = fieldError.getDefaultMessage();
                    Object rejectedValue = fieldError.getRejectedValue();

                    // Форматируем сообщение с указанием поля и ошибки
                    String detailedMessage = String.format(
                            ("Field '%s': %s (rejected value: %s)"),
                            fieldName,
                            errorMessage != null ? errorMessage : "Validation error",
                            rejectedValue != null ? rejectedValue.toString() : "null"
                    );
                    errors.put(fieldName, detailedMessage);
                } else if (error instanceof ConstraintViolation<?> violation) {
                    // Обработка ConstraintViolation, если есть
                    String path = violation.getPropertyPath().toString();
                    String message = violation.getMessage();
                    errors.put(path, message != null ? message : "Constraint violation");
                }
            }
        }

        String errorMessage;
        if (!errors.isEmpty()) {
            // Объединяем все ошибки в одну строку
            errorMessage = "Invalid input data: " + errors.entrySet().stream()
                    .map(e -> e.getKey() + " -> " + e.getValue())
                    .collect(Collectors.joining("; "));
        } else {
            // Если ошибок нет, используем общее сообщение
            errorMessage = "Validation failed: no specific field errors found";
        }

        ErrorResponse error = new ErrorResponse(
                "BAD_REQUEST",
                "Validation failed",
                errorMessage,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {

        ErrorResponse error = new ErrorResponse(
                "BAD_REQUEST",
                "Incorrectly made request.",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParameter(MissingServletRequestParameterException ex) {
        String paramName = ex.getParameterName();

        return new ErrorResponse(
                "BAD_REQUEST",
                "Incorrectly made request.",
                String.format("Required request parameter '%s' is not present", paramName),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ErrorResponse(
                "BAD_REQUEST",
                "Incorrectly made request.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }
}

