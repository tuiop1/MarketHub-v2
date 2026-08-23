package dev.tuiop.accountservice.customer.auth;

import dev.tuiop.accountservice.customer.Customer;
import dev.tuiop.accountservice.customer.CustomerRepository;
import dev.tuiop.accountservice.customer.CustomerService;
import dev.tuiop.accountservice.customer.dto.CustomerRegistrationRequest;
import dev.tuiop.accountservice.common.exceptions.EmailAlreadyTakenException;
import dev.tuiop.accountservice.kafka.AccountNotificationEventPublisher;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import dev.tuiop.accountservice.security.keycloak.RealmRole;
import dev.tuiop.commonevents.CustomerRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerRegistrationServiceTest {

    @Mock
    private KeycloakIdentityService identityService;
    @Mock
    private CustomerService customerService;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private AccountNotificationEventPublisher eventPublisher;

    @InjectMocks
    private CustomerRegistrationService customerRegistrationService;

    @Test
    void shouldDeleteKeycloakUserWhenRegistrationFails() {
        // Arrange
        String email = "  someemail@gmail.com    ";
        String normalizedEmail = "someemail@gmail.com";
        String keycloakId = "someStringKeycloakId";
        RuntimeException registrationFailure = new RuntimeException("someException");
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "John", "Doe", email, "password123", null, null
        );

        when(identityService.createUser(
                eq(normalizedEmail),
                anyString(),
                anyString(),
                anyString(),
                eq(RealmRole.CUSTOMER)
        )).thenReturn(keycloakId);
        when(customerService.create(keycloakId, normalizedEmail, request))
                .thenThrow(registrationFailure);

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> customerRegistrationService.register(request)
        );

        // Assert
        assertSame(registrationFailure, exception);
        verify(identityService).deleteUser(keycloakId);
        verify(eventPublisher, never()).publishCustomerRegistered(any());
    }

    @Test
    void shouldNotRegisterWhenEmailIsAlreadyTaken() {
        //arrange
        String normalizedEmail = "john@gmail.com";
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "John", "Doe", "  JOHN@gmail.com ", "password123", null, null
        );
        when(customerRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(true);

        // Act
        assertThrows(
                EmailAlreadyTakenException.class,
                () -> customerRegistrationService.register(request)
        );

        // Assert
        verify(merchantRepository, never()).existsByEmailIgnoreCase(normalizedEmail);
        verifyNoInteractions(identityService, customerService, eventPublisher);
    }

    @Test
    void shouldRegisterCustomerAndPublishEvent() {
        // Arrange
        String email = "  alex.brown@gmail.com  ";
        String normalizedEmail = "alex.brown@gmail.com";
        String keycloakId = "alexKeycloakId";
        UUID customerId = UUID.randomUUID();
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "Alex", "Brown", email, "password123", null, null
        );
        Customer customer = Customer.builder()
                .id(customerId)
                .keycloakUserId(keycloakId)
                .firstName("Alex")
                .lastName("Brown")
                .email(normalizedEmail)
                .enabled(true)
                .build();

        when(customerRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(merchantRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(identityService.createUser(
                normalizedEmail,
                "password123",
                "Alex",
                "Brown",
                RealmRole.CUSTOMER
        )).thenReturn(keycloakId);
        when(customerService.create(keycloakId, normalizedEmail, request)).thenReturn(customer);

        // Act
        Customer result = customerRegistrationService.register(request);

        // Assert
        assertSame(customer, result);

        ArgumentCaptor<CustomerRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(CustomerRegisteredEvent.class);
        verify(eventPublisher).publishCustomerRegistered(eventCaptor.capture());

        CustomerRegisteredEvent event = eventCaptor.getValue();
        assertEquals(customerId, event.customerId());
        assertEquals(normalizedEmail, event.email());
        assertEquals("Alex", event.firstName());
        assertNotNull(event.occurredAt());
        verify(identityService, never()).deleteUser(anyString());
    }
}
