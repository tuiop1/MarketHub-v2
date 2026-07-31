package dev.tuiop.orderservice.external.customers;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/api/v1/customers")
public interface AccountCustomerClient {

    @GetExchange("/me")
    CustomerResponse getMe(@RequestHeader("Authorization") String authorization);
}
