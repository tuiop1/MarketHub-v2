package dev.tuiop.accountservice.customer;

import dev.tuiop.accountservice.customer.dto.CustomerRegistrationRequest;
import dev.tuiop.accountservice.customer.mapper.CustomerMapper;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final KeycloakIdentityService keycloakIdentityService;

    @Transactional
    public Customer create(
            String keycloakUserId,
            CustomerRegistrationRequest request
    ) {
        Customer customer = customerMapper.toEntity(keycloakUserId, request);

        return customerRepository.save(customer);
    }


    public void enable(UUID customerId){

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer was not found with id:" + customerId));

        if(customer.getEnabled()){
            return;
        }
        try{

            enableInDB(customer);


        }
        catch (RuntimeException exception){
            throw exception;
        }

        keycloakIdentityService.addRealmRole(customer.getKeycloakUserId(),"CUSTOMER");
    }

    @Transactional
    public void enableInDB(Customer customer){



        customer.setEnabled(true);

        customerRepository.save(customer);

    }
}
