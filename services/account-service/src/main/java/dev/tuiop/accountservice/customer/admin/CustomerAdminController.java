package dev.tuiop.accountservice.customer.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasRole;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CustomerAdminController {

    private final CustomerAdminService customerAdminService;

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
