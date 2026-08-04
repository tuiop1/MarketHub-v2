package dev.tuiop.gatewayservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@WebFluxTest
@Import(SecurityConfig.class)
class InternalEndpointIsolationTests {

    private WebTestClient webTestClient;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUpWebTestClient() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedCallerCannotReachInternalStockEndpoint() {
        webTestClient.post()
                .uri("/internal/v1/catalog/stock-reservations")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void customerCannotReachInternalStockEndpoint() {
        webTestClient
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .post()
                .uri("/internal/v1/catalog/stock-reservations")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void customerCannotRefundAnotherCustomersPaymentThroughGateway() {
        webTestClient
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .post()
                .uri("/internal/v1/payments/{paymentId}/cancel-or-refund", UUID.randomUUID())
                .exchange()
                .expectStatus().isForbidden();
    }
}
