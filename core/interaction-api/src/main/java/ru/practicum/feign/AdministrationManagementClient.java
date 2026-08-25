package ru.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.compilations.NewCompilationDto;
import ru.practicum.dto.compilations.UpdateCompilationRequest;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.UpdateEventAdminRequest;
import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "administration", path = "/admin", configuration = FeignConfig.class)
public interface AdministrationManagementClient {

    @PostMapping("/categories")
    ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto dto);

    @PatchMapping("/categories/{catId}")
    ResponseEntity<CategoryDto> updateCategory(@PathVariable Long catId, @Valid @RequestBody CategoryDto dto);

    @DeleteMapping("/categories/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCategory(@PathVariable Long catId);

    @PostMapping("/compilations")
    @ResponseStatus(HttpStatus.CREATED)
    CompilationDto createCompilation(@Valid @RequestBody NewCompilationDto dto);

    @PatchMapping("/compilations/{compId}")
    CompilationDto updateCompilation(@Positive @PathVariable Long compId, @Valid @RequestBody UpdateCompilationRequest request);

    @DeleteMapping("/compilations/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCompilation(@Positive @PathVariable Long compId);

    @GetMapping("/events")
    ResponseEntity<List<EventFullDto>> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    );

    @PatchMapping("/events/{eventId}")
    ResponseEntity<EventFullDto> updateEventByAdmin(@PathVariable Long eventId, @Valid @RequestBody UpdateEventAdminRequest updateRequest);

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/users")
    UserDto createUser(@Valid @RequestBody NewUserRequest request);

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users")
    List<UserDto> get(
            @RequestParam(name = "ids", required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(defaultValue = "10") @Min(1) int size);

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/users{userId}")
    void delete(@PathVariable @Positive Long userId);
}
