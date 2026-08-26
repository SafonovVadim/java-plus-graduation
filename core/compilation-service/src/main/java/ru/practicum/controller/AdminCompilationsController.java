package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.compilations.NewCompilationDto;
import ru.practicum.dto.compilations.UpdateCompilationRequest;
import ru.practicum.feign.AdminCompilationsClient;
import ru.practicum.service.AdminCompilationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/compilations")
public class AdminCompilationsController implements AdminCompilationsClient {
    private final AdminCompilationService adminCompilationService;

    @Override
    public CompilationDto createCompilation(NewCompilationDto dto) {
        return adminCompilationService.createCompilation(dto);
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        return adminCompilationService.updateCompilation(compId, request);
    }

    @Override
    public void deleteCompilation(Long compId) {
        adminCompilationService.deleteCompilation(compId);
    }
}
