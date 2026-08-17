package ru.practicum.events.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.events.EventState;
import ru.practicum.events.Event;

import java.time.LocalDateTime;
import java.util.List;

public class EventSpecification {
    public static Specification<Event> hasStatePublished() {
        return (root, query, cb) -> cb.equal(root.get("state"), EventState.PUBLISHED);
    }

    public static Specification<Event> hasTextInAnnotationOrDescription(String text) {
        if (text == null || text.isBlank()) {
            return Specification.where(null);
        }
        String searchText = "%" + text.toLowerCase() + "%";
        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("annotation")), searchText),
                        cb.like(cb.lower(root.get("description")), searchText)
                );
    }

    public static Specification<Event> belongsToCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, cb) -> root.get("category").get("id").in(categoryIds);
    }

    public static Specification<Event> isPaid(Boolean paid) {
        if (paid == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("paid"), paid);
    }

    public static Specification<Event> isWithinRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (rangeStart != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }
            return predicate;
        };
    }
}
