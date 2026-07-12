package dev.tuiop.catalogservice.categories.mapper;


import dev.tuiop.catalogservice.categories.Category;
import dev.tuiop.catalogservice.categories.dto.CategoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}