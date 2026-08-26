package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.entity.User;

@FeignClient(name = "user-service", contextId = "publicUserClient", path = "/public/users", configuration = FeignConfig.class)
public interface PublicUserClient {

    @GetMapping("/{userId}")
    User getUser(@PathVariable Long userId);
}
