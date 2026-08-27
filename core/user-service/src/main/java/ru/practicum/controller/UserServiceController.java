package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;
import ru.practicum.entity.User;
import ru.practicum.feign.UserClient;
import ru.practicum.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserServiceController implements UserClient {

    private final UserService userService;

    @Override
    public UserDto createUser(NewUserRequest request) {
        return userService.save(request);
    }

    @Override
    public List<UserDto> get(List<Long> ids, int offset, int size) {
        return userService.findByIdsOrAllWithPagination(ids, offset, size);
    }

    @Override
    public void delete(@PathVariable Long userId) {
        userService.deleteById(userId);
    }
}
