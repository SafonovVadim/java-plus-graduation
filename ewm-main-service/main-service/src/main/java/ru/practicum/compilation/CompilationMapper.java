package ru.practicum.compilation;

import ru.practicum.events.Event;
import ru.practicum.events.EventsMapper;
import ru.practicum.events.dto.EventShortDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CompilationMapper {

    public static Compilation toCompilation(NewCompilationDto dto, List<Event> events) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
        compilation.setEvents(events);
        return compilation;
    }

    public static CompilationDto toCompilationDto(
            Compilation compilation,
            Map<Long, Long> confirmedRequestsMap,
            Map<Long, Long> viewsMap) {

        List<EventShortDto> shortEvents = compilation.getEvents().stream()
                .map(event -> {
                    EventShortDto shortDto = EventsMapper.toShortEventDto(
                            event,
                            confirmedRequestsMap.getOrDefault(event.getId(), 0L)
                    );
                    shortDto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return shortDto;
                })
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(shortEvents)
                .build();
    }

}
