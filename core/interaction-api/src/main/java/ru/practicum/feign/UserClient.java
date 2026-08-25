package ru.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;

import java.util.List;

@FeignClient(name = "administration", path = "/admin/users", configuration = FeignConfig.class)
public interface UserClient {

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping()
    UserDto createUser(@Valid @RequestBody NewUserRequest request);

    @ResponseStatus(HttpStatus.OK)
    @GetMapping()
    List<UserDto> get(
            @RequestParam(name = "ids", required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(defaultValue = "10") @Min(1) int size);

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{userId}")
    void delete(@PathVariable @Positive Long userId);
}
