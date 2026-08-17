package ru.practicum.events;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private Float lat;  // широта
    private Float lon;  // долгота
}
