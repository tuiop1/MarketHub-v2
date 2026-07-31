package dev.tuiop.catalogservice.products.dto;


import jakarta.validation.constraints.*;

import java.util.UUID;

public record UpdateProductRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 5000)
        String description,

        @NotNull
        @Positive
        Long priceCents,

        @NotNull
        @Min(0)
        Integer stockQuantity,

        @NotNull
        UUID categoryId,

        @NotNull
        Boolean active
) {
}