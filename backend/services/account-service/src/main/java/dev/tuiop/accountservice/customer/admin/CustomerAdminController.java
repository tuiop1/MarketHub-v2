package dev.tuiop.accountservice.customer.admin;

import dev.tuiop.accountservice.customer.dto.CustomerResponse;
import dev.tuiop.accountservice.customer.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CustomerAdminController {

    private final CustomerAdminService customerAdminService;
    private final CustomerMapper customerMapper;

    @GetMapping
    public Page<CustomerResponse> getCustomers(Pageable pageable) {
        return customerAdminService.getAll(pageable).map(customerMapper::toResponse);
    }

    @PatchMapping("/{customerId}/enable")
    public ResponseEntity<Void> enableCustomer(@PathVariable UUID customerId) {
        customerAdminService.enable(customerId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{customerId}/disable")
    public ResponseEntity<Void> disableCustomer(@PathVariable UUID customerId) {
        customerAdminService.disable(customerId);

        return ResponseEntity.noContent().build();
    }
}
