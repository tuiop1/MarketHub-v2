package dev.tuiop.catalogservice.categories;

import dev.tuiop.catalogservice.categories.dto.CreateCategoryRequest;
import dev.tuiop.catalogservice.categories.dto.UpdateCategoryRequest;
import dev.tuiop.catalogservice.categories.exceptions.CategoryAlreadyExistsException;
import dev.tuiop.catalogservice.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<Category> getActiveCategories(Pageable pageable) {
        return categoryRepository.findByActiveTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Category getCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId).filter(Category::getActive).orElseThrow(() -> new ResourceNotFoundException(Category.class, categoryId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<Category> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Category createCategory(CreateCategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new CategoryAlreadyExistsException(name);
        }
        Category category = Category.builder().name(name).description(request.description()).build();
        Category savedCategory = categoryRepository.save(category);
        log.info(
                "Category created: categoryId={}, name={}",
                savedCategory.getId(),
                savedCategory.getName()
        );
        return savedCategory;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Category updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException(Category.class, categoryId));
        String name = request.name().trim();
        categoryRepository.findByNameIgnoreCase(name).filter(existingCategory -> !existingCategory.getId().equals(categoryId)).ifPresent(existingCategory -> {
            throw new CategoryAlreadyExistsException(name);
        });
        category.update(name, request.description());
        log.info(
                "Category updated: categoryId={}, name={}",
                category.getId(),
                category.getName()
        );
        return category;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Category enableCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException(Category.class, categoryId));
        category.enable();
        log.info(
                "Category enabled: categoryId={}, name={}",
                category.getId(),
                category.getName()
        );
        return category;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Category disableCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException(Category.class, categoryId));
        category.disable();
        log.warn(
                "Category disabled: categoryId={}, name={}",
                category.getId(),
                category.getName()
        );
        return category;
    }
}
