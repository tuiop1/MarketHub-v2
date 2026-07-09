package dev.tuiop.cartservice.carts.orders;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/v1/orders")
public interface OrderClient {

    @PostExchange("/purchase")
    OrderResponse purchase(
            @RequestHeader("Authorization") String authorization,
            @RequestBody PurchaseRequest request
    );
}
