package dev.tuiop.accountservice.customer;

import dev.tuiop.accountservice.customer.dto.CustomerResponse;
import dev.tuiop.accountservice.customer.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor

public class CustomerController {

    private final CustomerMapper customerMapper;
    private final CustomerService customerService;


    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(customerMapper.toResponse(customerService.getById(id)));
    }

    @GetMapping("/keycloak/{keycloakUserId}")
    public ResponseEntity<CustomerResponse> getCustomerByKeycloakUserId(
            @PathVariable String keycloakUserId
    ) {
        return ResponseEntity.ok(customerMapper.toResponse(
                customerService.getByKeycloakUserId(keycloakUserId)
        ));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getMe(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(customerMapper.toResponse(customerService.getMe(jwt.getSubject())));

    }
}
