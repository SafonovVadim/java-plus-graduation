package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.entity.User;

@FeignClient(name = "additional-functionality", path = "/users", configuration = FeignConfig.class)
public interface PublicUserClient {

    User getUser(Long userId);

}
