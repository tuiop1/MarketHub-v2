package dev.tuiop.catalogservice.categories.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description
) {
}