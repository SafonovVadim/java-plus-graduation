    package ru.practicum.events;

    import jakarta.persistence.*;
    import lombok.*;
    import ru.practicum.categories.Category;
    import ru.practicum.user.User;

    import java.time.LocalDateTime;

    @Entity
    @Table(name = "events")
    @Getter
    @Setter
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class Event {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, length = 2000)
        private String annotation;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "category_id", nullable = false)
        private Category category;

        @Column(name = "confirmed_requests", nullable = false)
        private Long confirmedRequests = 0L;

        @Column(name = "created_on", nullable = false)
        private LocalDateTime createdOn;

        @Column(length = 7000)
        private String description;

        @Column(name = "event_date", nullable = false)
        private LocalDateTime eventDate;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "initiator_id", nullable = false)
        private User initiator;

        @Column(name = "location_lat")
        private Float locationLat;

        @Column(name = "location_lon")
        private Float locationLon;

        @Column(nullable = false)
        private Boolean paid;

        @Column(name = "participant_limit", nullable = false)
        private Integer participantLimit = 0;

        @Column(name = "published_on")
        private LocalDateTime publishedOn;

        @Column(name = "request_moderation", nullable = false)
        private Boolean requestModeration;

        @Enumerated(EnumType.STRING)
        @Column(length = 20, nullable = false)
        private EventState state = EventState.PENDING;

        @Column(nullable = false, length = 120)
        private String title;

        @Column(nullable = false)
        private Long views = 0L;
    }
