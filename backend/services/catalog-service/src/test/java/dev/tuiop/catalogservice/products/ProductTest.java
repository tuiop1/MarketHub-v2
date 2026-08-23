package dev.tuiop.catalogservice.products;

import dev.tuiop.catalogservice.categories.Category;
import dev.tuiop.catalogservice.products.exceptions.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @Test
    void shouldReduceStockWhenProductIsReserved() {
        // Arrange
        Product product = activeProductWithStock(7);

        // Act
        product.reserveStock(2);

        // Assert
        assertEquals(5, product.getStockQuantity());
    }

    @Test
    void shouldKeepStockWhenRequestedQuantityIsNotAvailable() {
        // Arrange
        Product product = activeProductWithStock(2);

        // Act
        assertThrows(InsufficientStockException.class, () -> product.reserveStock(3));

        // Assert
        assertEquals(2, product.getStockQuantity());
    }

    private Product activeProductWithStock(int stockQuantity) {
        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("Coffee")
                .active(true)
                .build();

        return Product.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .category(category)
                .name("Coffee beans")
                .priceCents(1_299L)
                .stockQuantity(stockQuantity)
                .active(true)
                .build();
    }
}
