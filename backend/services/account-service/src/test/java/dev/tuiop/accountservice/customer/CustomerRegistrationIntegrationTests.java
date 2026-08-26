package dev.tuiop.accountservice.customer;

import dev.tuiop.accountservice.kafka.AccountNotificationEventPublisher;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import dev.tuiop.accountservice.security.keycloak.RealmRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
        "spring.kafka.bootstrap-servers=localhost:1",
        "keycloak.admin.client-secret=test-secret"
})
class CustomerRegistrationIntegrationTests {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.14-alpine3.24")
            .withDatabaseName("account_test")
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
    private CustomerRepository customerRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @MockitoBean
    private KeycloakIdentityService keycloakIdentityService;

    @MockitoBean
    private AccountNotificationEventPublisher eventPublisher;

    @BeforeEach
    void clearDatabaseAndMocks() {
        merchantRepository.deleteAll();
        customerRepository.deleteAll();
        reset(keycloakIdentityService, eventPublisher);
    }

    @Test
    void registrationPersistsNormalizedCustomerAndRejectsDuplicateEmail() throws Exception {
        when(keycloakIdentityService.createUser(
                "alice@example.com",
                "password123",
                "Alice",
                "Brown",
                RealmRole.CUSTOMER
        )).thenReturn("keycloak-alice");

        mockMvc.perform(post("/api/v1/auth/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest("Alice@Example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.enabled").value(true));

        Customer persisted = customerRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(persisted.getKeycloakUserId()).isEqualTo("keycloak-alice");
        assertThat(persisted.getCreatedAt()).isNotNull();
        verify(eventPublisher).publishCustomerRegistered(argThat(event ->
                event.customerId().equals(persisted.getId())
                        && event.email().equals("alice@example.com")
        ));

        mockMvc.perform(post("/api/v1/auth/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest("ALICE@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_TAKEN"));

        assertThat(customerRepository.count()).isOne();
        verify(keycloakIdentityService, times(1)).createUser(
                "alice@example.com",
                "password123",
                "Alice",
                "Brown",
                RealmRole.CUSTOMER
        );
    }

    private String registrationRequest(String email) {
        return """
                {
                  "firstName": "Alice",
                  "lastName": "Brown",
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email);
    }
}
