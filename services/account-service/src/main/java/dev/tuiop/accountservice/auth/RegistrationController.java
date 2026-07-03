package dev.tuiop.accountservice.auth;

import dev.tuiop.accountservice.customer.auth.CustomerRegistrationService;
import dev.tuiop.accountservice.customer.dto.CustomerRegistrationRequest;
import dev.tuiop.accountservice.customer.dto.CustomerResponse;
import dev.tuiop.accountservice.merchant.auth.MerchantRegistrationService;
import dev.tuiop.accountservice.merchant.dto.MerchantRegistrationRequest;
import dev.tuiop.accountservice.merchant.dto.MerchantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final CustomerRegistrationService customerRegistrationService;
    private final MerchantRegistrationService merchantRegistrationService;

    @PostMapping("/customers/register")
    @ResponseStatus(HttpStatus.CREATED)
    CustomerResponse registerCustomer(
            @Valid @RequestBody CustomerRegistrationRequest request
    ) {
        return customerRegistrationService.register(request);
    }

    @PostMapping("/merchants/register")
    @ResponseStatus(HttpStatus.CREATED)
    MerchantResponse registerMerchant(
            @Valid @RequestBody MerchantRegistrationRequest request
    ) {
        return merchantRegistrationService.register(request);
    }
}
