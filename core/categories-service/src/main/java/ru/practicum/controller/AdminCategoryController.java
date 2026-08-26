package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.compilations.NewCompilationDto;
import ru.practicum.feign.AdminCategoriesClient;
import ru.practicum.service.AdminCategoryService;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController implements AdminCategoriesClient {
    private final AdminCategoryService adminCategoryService;

    @Override
    public ResponseEntity<CategoryDto> createCategory(CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCategoryService.createCategory(dto));
    }

    @Override
    public ResponseEntity<CategoryDto> updateCategory(Long catId, CategoryDto dto) {
        return ResponseEntity.ok(adminCategoryService.updateCategory(catId, dto));
    }

    @Override
    public void deleteCategory(Long catId) {
        adminCategoryService.deleteCategory(catId);
    }
}
