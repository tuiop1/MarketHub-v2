package dev.tuiop.accountservice.customer;

import dev.tuiop.accountservice.customer.dto.CustomerRegistrationRequest;
import dev.tuiop.accountservice.customer.exceptions.CustomerAlreadyExistsException;
import dev.tuiop.accountservice.customer.mapper.CustomerMapper;
import dev.tuiop.accountservice.common.exceptions.EmailAlreadyTakenException;
import dev.tuiop.accountservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final CustomerMapper customerMapper;

    @Transactional
    public Customer create(
            String keycloakUserId,
            String email,
            CustomerRegistrationRequest request
    ) {

        if (customerRepository.existsByEmailIgnoreCase(email) || merchantRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyTakenException(email);
        }

        if (customerRepository.existsByKeycloakUserId(keycloakUserId)) {
            throw new CustomerAlreadyExistsException();
        }

        Customer customer = customerMapper.toEntity(keycloakUserId, email, request);

        Customer savedCustomer = customerRepository.save(customer);

        UUID customerId = savedCustomer.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Customer profile created during registration: customerId={}", customerId);
            }
        });

        return savedCustomer;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Transactional(readOnly = true)
    public Customer getMe(String keycloakUserId) {
        return getByKeycloakUserId(keycloakUserId);
    }

    @Transactional(readOnly = true)
    public Customer getById(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.class, customerId));
    }

    @Transactional(readOnly = true)
    public Customer getByKeycloakUserId(String keycloakUserId) {
        return customerRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Customer.class,
                        "keycloakUserId",
                        keycloakUserId
                ));
    }


}
