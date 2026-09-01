package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.feign.AdminCategoriesClient;
import ru.practicum.service.AdminCategoryService;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController implements AdminCategoriesClient {
    private final AdminCategoryService adminCategoryService;

    @Override
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCategoryService.createCategory(dto));
    }

    @Override
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long catId, @RequestBody CategoryDto dto) {
        return ResponseEntity.ok(adminCategoryService.updateCategory(catId, dto));
    }

    @Override
    public void deleteCategory(@PathVariable Long catId) {
        adminCategoryService.deleteCategory(catId);
    }
}
