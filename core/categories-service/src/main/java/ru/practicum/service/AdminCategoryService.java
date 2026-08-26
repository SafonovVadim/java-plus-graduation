package ru.practicum.service;

import ru.practicum.dto.categories.CategoryDto;

public interface AdminCategoryService {

    CategoryDto createCategory(CategoryDto dto);

    CategoryDto updateCategory(Long catId, CategoryDto dto);

    void deleteCategory(Long catId);

}
