package dev.tuiop.catalogservice.categories;

import dev.tuiop.catalogservice.cache.ProductCacheInvalidator;
import dev.tuiop.catalogservice.categories.dto.CategoryResponse;
import dev.tuiop.catalogservice.categories.dto.CreateCategoryRequest;
import dev.tuiop.catalogservice.categories.exceptions.CategoryAlreadyExistsException;
import dev.tuiop.catalogservice.categories.mapper.CategoryMapper;
import dev.tuiop.catalogservice.products.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductCacheInvalidator productCacheInvalidator;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateCategoryWithTrimmedName() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "  Coffee  ",
                "Coffee, tea and accessories"
        );
        Category savedCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Coffee")
                .description(request.description())
                .active(true)
                .build();
        CategoryResponse expectedResponse = new CategoryResponse(
                savedCategory.getId(),
                "Coffee",
                request.description(),
                true,
                Instant.now()
        );

        when(categoryRepository.existsByNameIgnoreCase("Coffee")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toResponse(savedCategory)).thenReturn(expectedResponse);

        // Act
        CategoryResponse result = categoryService.createCategory(request);

        // Assert
        assertSame(expectedResponse, result);

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertEquals("Coffee", categoryCaptor.getValue().getName());
        assertEquals(request.description(), categoryCaptor.getValue().getDescription());
    }

    @Test
    void shouldRejectCategoryWhenNameIsAlreadyTaken() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "  Electronics ",
                "Phones and computers"
        );
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        // Act
        assertThrows(
                CategoryAlreadyExistsException.class,
                () -> categoryService.createCategory(request)
        );

        // Assert
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(categoryMapper);
    }
}
