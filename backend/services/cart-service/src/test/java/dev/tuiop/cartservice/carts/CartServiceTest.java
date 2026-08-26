package dev.tuiop.cartservice.carts;

import dev.tuiop.cartservice.carts.dto.AddToCartRequest;
import dev.tuiop.cartservice.carts.exceptions.InsufficientStockException;
import dev.tuiop.cartservice.carts.item.CartItemRepository;
import dev.tuiop.cartservice.external.categories.CatalogCategoryClient;
import dev.tuiop.cartservice.external.categories.CategoryResponse;
import dev.tuiop.cartservice.external.customers.AccountCustomerClient;
import dev.tuiop.cartservice.external.customers.CustomerResponse;
import dev.tuiop.cartservice.external.merchants.AccountMerchantClient;
import dev.tuiop.cartservice.external.merchants.MerchantResponse;
import dev.tuiop.cartservice.external.merchants.MerchantStatus;
import dev.tuiop.cartservice.external.products.CatalogProductClient;
import dev.tuiop.cartservice.external.products.InternalCatalogProductClient;
import dev.tuiop.cartservice.external.products.ProductResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private AccountCustomerClient accountCustomerClient;
    @Mock
    private CatalogProductClient catalogProductClient;
    @Mock
    private InternalCatalogProductClient internalCatalogProductClient;
    @Mock
    private AccountMerchantClient accountMerchantClient;
    @Mock
    private CatalogCategoryClient catalogCategoryClient;

    @InjectMocks
    private CartService cartService;

    @Test
    void shouldReturnCartForLoggedInCustomer() {
        // Arrange
        Jwt jwt = org.mockito.Mockito.mock(Jwt.class);
        UUID customerId = UUID.randomUUID();
        CustomerResponse customer = customer(customerId);
        Cart cart = Cart.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .build();

        when(jwt.getTokenValue()).thenReturn("someToken");
        when(accountCustomerClient.getMe("Bearer someToken")).thenReturn(customer);
        when(cartRepository.findCartByCustomerId(customerId)).thenReturn(Optional.of(cart));

        // Act
        Cart result = cartService.getMyCart(jwt);

        // Assert
        assertSame(cart, result);
    }

    @Test
    void shouldRejectQuantityThatIsHigherThanAvailableStock() {
        // Arrange
        Jwt jwt = org.mockito.Mockito.mock(Jwt.class);
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        AddToCartRequest request = new AddToCartRequest(productId, 3);

        CustomerResponse customer = customer(customerId);
        ProductResponse product = new ProductResponse(
                productId,
                merchantId,
                "Coffee House",
                categoryId,
                "Coffee beans",
                "Medium roast",
                1_299L,
                2,
                true,
                null,
                null
        );
        CategoryResponse category = new CategoryResponse(
                categoryId, "Coffee", "Coffee and tea", true, null
        );
        MerchantResponse merchant = new MerchantResponse(
                merchantId,
                "Coffee House",
                "Coffee and fresh pastries",
                "maria@gmail.com",
                MerchantStatus.VERIFIED,
                null,
                null,
                null
        );

        when(jwt.getTokenValue()).thenReturn("someToken");
        when(accountCustomerClient.getMe("Bearer someToken")).thenReturn(customer);
        when(catalogProductClient.getProductById(productId)).thenReturn(product);
        when(catalogCategoryClient.getCategoryById(categoryId)).thenReturn(category);
        when(accountMerchantClient.getMerchantById(merchantId)).thenReturn(merchant);

        // Act
        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addProductToMyCart(request, jwt)
        );

        // Assert
        verifyNoInteractions(cartRepository, cartItemRepository);
    }

    private CustomerResponse customer(UUID customerId) {
        return new CustomerResponse(
                customerId,
                "John",
                "Doe",
                null,
                "john@gmail.com",
                null,
                true,
                null,
                null
        );
    }
}
