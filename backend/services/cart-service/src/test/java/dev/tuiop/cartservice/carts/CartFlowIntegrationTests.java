package dev.tuiop.cartservice.carts;

import dev.tuiop.cartservice.carts.item.CartItemRepository;
import dev.tuiop.cartservice.external.categories.CatalogCategoryClient;
import dev.tuiop.cartservice.external.categories.CategoryResponse;
import dev.tuiop.cartservice.external.customers.AccountCustomerClient;
import dev.tuiop.cartservice.external.customers.CustomerResponse;
import dev.tuiop.cartservice.external.merchants.AccountMerchantClient;
import dev.tuiop.cartservice.external.merchants.MerchantResponse;
import dev.tuiop.cartservice.external.merchants.MerchantStatus;
import dev.tuiop.cartservice.external.products.CatalogProductClient;
import dev.tuiop.cartservice.external.products.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused"
})
class CartFlowIntegrationTests {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.14-alpine3.24")
            .withDatabaseName("cart_test")
            .withUsername("markethub")
            .withPassword("markethub");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @MockitoBean
    private AccountCustomerClient accountCustomerClient;

    @MockitoBean
    private CatalogProductClient catalogProductClient;

    @MockitoBean
    private CatalogCategoryClient catalogCategoryClient;

    @MockitoBean
    private AccountMerchantClient accountMerchantClient;

    private UUID customerId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        reset(
                accountCustomerClient,
                catalogProductClient,
                catalogCategoryClient,
                accountMerchantClient
        );

        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        when(accountCustomerClient.getMe("Bearer customer-token"))
                .thenReturn(customer(customerId));
        when(catalogProductClient.getProductById(productId))
                .thenReturn(product(productId, categoryId, merchantId));
        when(catalogCategoryClient.getCategoryById(categoryId))
                .thenReturn(new CategoryResponse(
                        categoryId, "Coffee", "Coffee and tea", true, null
                ));
        when(accountMerchantClient.getMerchantById(merchantId))
                .thenReturn(new MerchantResponse(
                        merchantId,
                        "Coffee House",
                        "Coffee and fresh pastries",
                        "merchant@example.com",
                        MerchantStatus.VERIFIED,
                        null,
                        null,
                        null
                ));
    }

    @Test
    void addingTheSameProductCreatesOneCartAndIncreasesQuantity() throws Exception {
        addProduct(1, 1);
        addProduct(2, 3);

        assertThat(cartRepository.count()).isOne();
        Cart persistedCart = cartRepository.findCartByCustomerId(customerId).orElseThrow();
        assertThat(persistedCart.getCartItems())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getProductId()).isEqualTo(productId);
                    assertThat(item.getQuantity()).isEqualTo(3);
                });
    }

    @Test
    void unauthenticatedCustomerCannotChangeCart() throws Exception {
        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemRequest(1)))
                .andExpect(status().isUnauthorized());

        assertThat(cartItemRepository.count()).isZero();
    }

    private void addProduct(int quantity, int expectedQuantity) throws Exception {
        mockMvc.perform(post("/api/v1/carts/items")
                        .with(jwt()
                                .jwt(builder -> builder.tokenValue("customer-token"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemRequest(quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantity").value(expectedQuantity))
                .andExpect(jsonPath("$.totalPriceCents").value(1_250L * expectedQuantity));
    }

    private String addItemRequest(int quantity) {
        return """
                {
                  "productId": "%s",
                  "quantity": %d
                }
                """.formatted(productId, quantity);
    }

    private CustomerResponse customer(UUID id) {
        return new CustomerResponse(
                id,
                "Taylor",
                "Customer",
                null,
                "customer@example.com",
                null,
                true,
                null,
                null
        );
    }

    private ProductResponse product(UUID id, UUID categoryId, UUID merchantId) {
        return new ProductResponse(
                id,
                merchantId,
                "Coffee House",
                categoryId,
                "Coffee beans",
                "Medium roast",
                1_250L,
                5,
                true,
                null,
                null
        );
    }
}
