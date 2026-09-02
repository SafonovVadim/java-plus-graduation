package ru.practicum;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.entity.UserAction;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
}
