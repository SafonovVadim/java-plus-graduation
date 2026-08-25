package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.compilations.NewCompilationDto;
import ru.practicum.dto.compilations.UpdateCompilationRequest;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.UpdateEventAdminRequest;
import ru.practicum.feign.AdministrationManagementClient;
import ru.practicum.service.AdministrationService;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdministrationController implements AdministrationManagementClient {
    private final AdministrationService administrationService;

    @Override
    public ResponseEntity<CategoryDto> createCategory(CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(administrationService.createCategory(dto));
    }

    @Override
    public ResponseEntity<CategoryDto> updateCategory(Long catId, CategoryDto dto) {
        return ResponseEntity.ok(administrationService.updateCategory(catId, dto));
    }

    @Override
    public void deleteCategory(Long catId) {
        administrationService.deleteCategory(catId);
    }

    @Override
    public CompilationDto createCompilation(NewCompilationDto dto) {
        return administrationService.createCompilation(dto);
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        return administrationService.updateCompilation(compId, request);
    }

    @Override
    public void deleteCompilation(Long compId) {
        administrationService.deleteCompilation(compId);
    }

    @Override
    public ResponseEntity<List<EventFullDto>> getEvents(List<Long> users, List<String> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        return ResponseEntity.ok(administrationService.getEvents(users, states, categories, rangeStart, rangeEnd, from, size));
    }

    @Override
    public ResponseEntity<EventFullDto> updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        return ResponseEntity.ok(administrationService.updateEventByAdmin(eventId, updateRequest));
    }

}
