package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.users.UserDto;

@FeignClient(name = "user-service", contextId = "publicUserClient", path = "/public/users", configuration = FeignConfig.class)
public interface PublicUserClient {

    @GetMapping("/{userId}")
    UserDto getUser(@PathVariable Long userId);
}
