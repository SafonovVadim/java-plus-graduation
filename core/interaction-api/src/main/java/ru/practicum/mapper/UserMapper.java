package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;
import ru.practicum.dto.users.UserShortDto;
import ru.practicum.entity.User;

@Component
public class UserMapper {

    public static User toEntity(NewUserRequest request) {
        return request == null ? null
                : new User(request.getName(), request.getEmail());
    }

    public static UserDto toDto(User user) {
        return user == null ? null
                : new UserDto(user.getId(),  user.getName(), user.getEmail());
    }

    public static UserShortDto toShortDto(User user) {
        return user == null ? null
                : new UserShortDto(user.getId(), user.getName());
    }
}

