package ru.practicum;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.entity.UserAction;

import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
}
