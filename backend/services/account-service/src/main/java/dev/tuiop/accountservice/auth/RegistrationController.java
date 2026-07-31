package dev.tuiop.accountservice.auth;

import dev.tuiop.accountservice.customer.Customer;
import dev.tuiop.accountservice.customer.auth.CustomerRegistrationService;
import dev.tuiop.accountservice.customer.dto.CustomerRegistrationRequest;
import dev.tuiop.accountservice.customer.dto.CustomerResponse;
import dev.tuiop.accountservice.customer.mapper.CustomerMapper;
import dev.tuiop.accountservice.merchant.Merchant;
import dev.tuiop.accountservice.merchant.auth.MerchantRegistrationService;
import dev.tuiop.accountservice.merchant.dto.MerchantRegistrationRequest;
import dev.tuiop.accountservice.merchant.dto.MerchantResponse;
import dev.tuiop.accountservice.merchant.mapper.MerchantMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final CustomerRegistrationService customerRegistrationService;
    private final MerchantRegistrationService merchantRegistrationService;
    private final CustomerMapper customerMapper;
    private final MerchantMapper merchantMapper;

    @PostMapping("/customers/register")
    public ResponseEntity<CustomerResponse> registerCustomer(
            @Valid @RequestBody CustomerRegistrationRequest request
    ) {
        Customer customer = customerRegistrationService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(customerMapper.toResponse(customer));
    }

    @PostMapping("/merchants/register")
    public ResponseEntity<MerchantResponse> registerMerchant(
            @Valid @RequestBody MerchantRegistrationRequest request
    ) {
        Merchant merchant = merchantRegistrationService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(merchantMapper.toResponse(merchant));
    }
}
