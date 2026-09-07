package ru.practicum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.entity.UserAction;

import java.util.List;
import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    List<UserAction> findByUserId(Long userId);

    @Query("SELECT ua.eventId, COALESCE(SUM(ua.totalWeight), 0) FROM UserAction ua WHERE ua.eventId IN :eventIds GROUP BY ua.eventId")
    List<Object[]> sumMaxWeightByEventIdsGrouped(@Param("eventIds") List<Long> eventIds);
}
