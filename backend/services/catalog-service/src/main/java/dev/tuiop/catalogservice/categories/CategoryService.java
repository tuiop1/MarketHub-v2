package dev.tuiop.catalogservice.categories;

import dev.tuiop.catalogservice.categories.dto.CreateCategoryRequest;
import dev.tuiop.catalogservice.categories.dto.CategoryResponse;
import dev.tuiop.catalogservice.categories.dto.UpdateCategoryRequest;
import dev.tuiop.catalogservice.categories.exceptions.CategoryAlreadyExistsException;
import dev.tuiop.catalogservice.categories.mapper.CategoryMapper;
import dev.tuiop.catalogservice.cache.ProductCacheInvalidator;
import dev.tuiop.catalogservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.catalogservice.products.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepository;
    private final ProductCacheInvalidator productCacheInvalidator;

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getActiveCategories(Pageable pageable) {
        return categoryRepository.findByActiveTrue(pageable).map(categoryMapper::toResponse);
    }

    @Cacheable(value = "category", key = "#categoryId.toString()")
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .filter(Category::getActive)
                .orElseThrow(() -> new ResourceNotFoundException(Category.class, categoryId));
        return categoryMapper.toResponse(category);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(categoryMapper::toResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
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
        return categoryMapper.toResponse(savedCategory);
    }

    @CacheEvict(value = "category", key = "#categoryId.toString()")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request) {
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
        return categoryMapper.toResponse(category);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CategoryResponse enableCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException(Category.class, categoryId));
        category.enable();
        log.info(
                "Category enabled: categoryId={}, name={}",
                category.getId(),
                category.getName()
        );
        return categoryMapper.toResponse(category);
    }

    @CacheEvict(value = "category", key = "#categoryId.toString()")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CategoryResponse disableCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException(Category.class, categoryId));
        category.disable();
        productCacheInvalidator.evict(productRepository.findIdsByCategoryId(categoryId));
        log.info(
                "Category disabled: categoryId={}, name={}",
                category.getId(),
                category.getName()
        );
        return categoryMapper.toResponse(category);
    }
}
