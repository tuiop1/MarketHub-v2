package dev.tuiop.accountservice.customer;

import dev.tuiop.accountservice.customer.dto.CustomerRegistrationRequest;
import dev.tuiop.accountservice.customer.exceptions.CustomerAlreadyExistsException;
import dev.tuiop.accountservice.customer.mapper.CustomerMapper;
import dev.tuiop.accountservice.common.exceptions.EmailAlreadyTakenException;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final CustomerMapper customerMapper;

    @Transactional
    public Customer create(
            String keycloakUserId,
            CustomerRegistrationRequest request
    ) {
        String email = request.email().trim().toLowerCase();

        if (customerRepository.existsByEmailIgnoreCase(email) || merchantRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyTakenException(email);
        }

        if (customerRepository.existsByKeycloakUserId(keycloakUserId)) {
            throw new CustomerAlreadyExistsException();
        }

        Customer customer = customerMapper.toEntity(keycloakUserId, request);

        return customerRepository.save(customer);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Transactional(readOnly = true)
    public Customer getMe(String keycloakUserId){





        return getByKeycloakUserId(keycloakUserId);

    }

    private Customer getByKeycloakUserId(String keycloakUserId){
        Customer toReturn = customerRepository.findByKeycloakUserId(keycloakUserId).orElseThrow(() -> new EntityNotFoundException("Customer was not found with keycloakUserId: " + keycloakUserId));
        return  toReturn;
    }

}
