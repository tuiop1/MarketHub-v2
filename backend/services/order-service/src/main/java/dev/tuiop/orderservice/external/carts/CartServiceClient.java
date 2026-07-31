package dev.tuiop.orderservice.external.carts;

import dev.tuiop.orderservice.external.carts.dto.CartResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/api/v1/carts")
public interface CartServiceClient {




    @GetExchange
    CartResponse getMyCart(@RequestHeader("Authorization") String authorization);

    @DeleteExchange("/items")
    void clearMyCart(@RequestHeader("Authorization") String authorization);

}
