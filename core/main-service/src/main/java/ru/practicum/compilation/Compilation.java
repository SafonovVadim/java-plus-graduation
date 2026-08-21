package ru.practicum.compilation;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.events.Event;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compilations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "pinned", nullable = false)
    private Boolean pinned;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToMany
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private List<Event> events = new ArrayList<>();

    public Compilation(String title, Boolean pinned, String description) {
        this.title = title;
        this.pinned = pinned;
        this.description = description;
    }
}
