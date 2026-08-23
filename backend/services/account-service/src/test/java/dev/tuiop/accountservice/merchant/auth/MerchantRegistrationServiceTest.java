package dev.tuiop.accountservice.merchant.auth;

import dev.tuiop.accountservice.customer.CustomerRepository;
import dev.tuiop.accountservice.kafka.AccountNotificationEventPublisher;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import dev.tuiop.accountservice.merchant.MerchantService;
import dev.tuiop.accountservice.merchant.dto.MerchantRegistrationRequest;
import dev.tuiop.accountservice.merchant.exceptions.MerchantShopNameTakenException;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantRegistrationServiceTest {

    @Mock
    private KeycloakIdentityService identityService;
    @Mock
    private MerchantService merchantService;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private AccountNotificationEventPublisher eventPublisher;

    @InjectMocks
    private MerchantRegistrationService merchantRegistrationService;

    @Test
    void shouldNotCreateUserWhenShopNameIsTaken() {
        // Arrange
        String normalizedEmail = "maria@gmail.com";
        String shopName = "Coffee House";
        MerchantRegistrationRequest request = new MerchantRegistrationRequest(
                "Maria",
                "Green",
                "  MARIA@gmail.com ",
                "password123",
                "  Coffee House  ",
                "Coffee and fresh pastries"
        );

        when(customerRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(merchantRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(merchantRepository.existsByShopNameIgnoreCase(shopName)).thenReturn(true);

        // Act
        assertThrows(
                MerchantShopNameTakenException.class,
                () -> merchantRegistrationService.register(request)
        );

        // Assert
        verifyNoInteractions(identityService, merchantService, eventPublisher);
    }
}
