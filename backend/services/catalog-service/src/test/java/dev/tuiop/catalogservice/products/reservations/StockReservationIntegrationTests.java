package dev.tuiop.catalogservice.products.reservations;

import dev.tuiop.catalogservice.cache.ProductCacheInvalidator;
import dev.tuiop.catalogservice.categories.Category;
import dev.tuiop.catalogservice.categories.CategoryRepository;
import dev.tuiop.catalogservice.products.Product;
import dev.tuiop.catalogservice.products.ProductRepository;
import dev.tuiop.catalogservice.products.exceptions.InsufficientStockException;
import dev.tuiop.catalogservice.products.reservations.dto.StockReservationItemRequest;
import dev.tuiop.catalogservice.products.reservations.dto.StockReservationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused"
})
class StockReservationIntegrationTests {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.14-alpine3.24")
            .withDatabaseName("catalog_test")
            .withUsername("markethub")
            .withPassword("markethub");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ProductCacheInvalidator productCacheInvalidator;

    private ExecutorService executor;

    @BeforeEach
    void clearDatabase() {
        stockReservationRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @AfterEach
    void stopExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void reservingStockDecrementsProductAndStoresPriceSnapshot() {
        Product product = createProduct(5, 1_250L);
        UUID reservationId = UUID.randomUUID();

        StockReservation reservation = stockReservationService.reserveStock(request(reservationId, product, 2));

        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.RESERVED);
        assertThat(reservation.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductId()).isEqualTo(product.getId());
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getUnitPriceCents()).isEqualTo(1_250L);
            assertThat(item.getTotalPriceCents()).isEqualTo(2_500L);
        });
        assertThat(currentStock(product)).isEqualTo(3);
    }

    @Test
    void releasingReservationRestoresStock() {
        Product product = createProduct(3, 1_250L);
        UUID reservationId = UUID.randomUUID();
        stockReservationService.reserveStock(request(reservationId, product, 2));

        stockReservationService.releaseReservation(reservationId);

        assertThat(currentStock(product)).isEqualTo(3);
        assertThat(stockReservationRepository.findById(reservationId))
                .get()
                .extracting(StockReservation::getStatus)
                .isEqualTo(StockReservationStatus.RELEASED);
    }

    @Test
    void duplicateReservationRequestDoesNotDecrementStockTwice() {
        Product product = createProduct(4, 1_250L);
        UUID reservationId = UUID.randomUUID();
        StockReservationRequest request = request(reservationId, product, 2);

        StockReservation first = stockReservationService.reserveStock(request);
        StockReservation duplicate = stockReservationService.reserveStock(request);

        assertThat(duplicate.getId()).isEqualTo(first.getId());
        assertThat(currentStock(product)).isEqualTo(2);
        assertThat(stockReservationRepository.count()).isOne();
    }

    @Test
    void duplicateProductLinesAreMergedBeforeStockIsReserved() {
        Product product = createProduct(5, 1_250L);
        UUID reservationId = UUID.randomUUID();
        StockReservationRequest request = new StockReservationRequest(
                reservationId,
                List.of(
                        new StockReservationItemRequest(product.getId(), 1),
                        new StockReservationItemRequest(product.getId(), 2)
                )
        );

        StockReservation reservation = stockReservationService.reserveStock(request);

        assertThat(reservation.getItems()).singleElement()
                .extracting(StockReservationItem::getQuantity)
                .isEqualTo(3);
        assertThat(currentStock(product)).isEqualTo(2);
    }

    @Test
    void releasingReservationTwiceIsIdempotent() {
        Product product = createProduct(3, 1_250L);
        UUID reservationId = UUID.randomUUID();
        stockReservationService.reserveStock(request(reservationId, product, 2));

        stockReservationService.releaseReservation(reservationId);
        stockReservationService.releaseReservation(reservationId);

        assertThat(currentStock(product)).isEqualTo(3);
    }

    @Test
    void committedReservationIsIdempotentAndCannotBeReleased() {
        Product product = createProduct(3, 1_250L);
        UUID reservationId = UUID.randomUUID();
        stockReservationService.reserveStock(request(reservationId, product, 2));

        stockReservationService.commitReservation(reservationId);
        stockReservationService.commitReservation(reservationId);

        assertThatThrownBy(() -> stockReservationService.releaseReservation(reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Committed reservation");
        assertThat(currentStock(product)).isEqualTo(1);
    }

    @Test
    void insufficientStockRollsBackReservation() {
        Product product = createProduct(1, 1_250L);
        UUID reservationId = UUID.randomUUID();

        assertThatThrownBy(() -> stockReservationService.reserveStock(request(reservationId, product, 2)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(currentStock(product)).isOne();
        assertThat(stockReservationRepository.existsById(reservationId)).isFalse();
    }

    @Test
    void concurrentOrdersForLastItemAllowExactlyOneReservation() throws Exception {
        Product product = createProduct(1, 1_250L);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        executor = Executors.newFixedThreadPool(2);

        Future<Boolean> first = executor.submit(() -> reserveAfterSignal(product, ready, start));
        Future<Boolean> second = executor.submit(() -> reserveAfterSignal(product, ready, start));
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        assertThat(List.of(
                first.get(10, TimeUnit.SECONDS),
                second.get(10, TimeUnit.SECONDS)
        ))
                .containsExactlyInAnyOrder(true, false);
        assertThat(currentStock(product)).isZero();
        assertThat(stockReservationRepository.count()).isOne();
    }

    private boolean reserveAfterSignal(Product product, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            stockReservationService.reserveStock(request(UUID.randomUUID(), product, 1));
            return true;
        } catch (InsufficientStockException exception) {
            return false;
        }
    }

    private StockReservationRequest request(UUID reservationId, Product product, int quantity) {
        return new StockReservationRequest(
                reservationId,
                List.of(new StockReservationItemRequest(product.getId(), quantity))
        );
    }

    private Product createProduct(int stock, long priceCents) {
        Category category = categoryRepository.saveAndFlush(Category.builder()
                .name("Category-" + UUID.randomUUID())
                .description("Test category")
                .active(true)
                .build());

        return productRepository.saveAndFlush(Product.builder()
                .merchantId(UUID.randomUUID())
                .category(category)
                .name("Product-" + UUID.randomUUID())
                .description("Test product")
                .priceCents(priceCents)
                .stockQuantity(stock)
                .active(true)
                .build());
    }

    private int currentStock(Product product) {
        return productRepository.findById(product.getId()).orElseThrow().getStockQuantity();
    }
}
