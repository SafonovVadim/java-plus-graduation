package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.CategoryRepository;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.entity.Category;
import ru.practicum.errors.exception.ConflictException;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;

@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements AdminCategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public CategoryDto createCategory(CategoryDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ConflictException("Category name cannot be empty");
        }

        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name '" + dto.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(dto.getName())
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toCategoryDto(saved);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));

        if (dto.getName() != null && !dto.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(dto.getName())) {
                throw new ConflictException("Category with name '" + dto.getName() + "' already exists");
            }
            category.setName(dto.getName());
        }

        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    public void deleteCategory(Long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Category with id=" + catId + " was not found");
        }

        int deletedCount = categoryRepository.deleteCategoryIfNotUsed(catId);

        if (deletedCount == 0) {
            throw new ConflictException("Category is used by events and cannot be deleted");
        }
    }
}
