package dev.tuiop.orderservice.orders;

import dev.tuiop.orderservice.customers.CustomerResponse;
import dev.tuiop.orderservice.orders.dto.OrderResponse;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.payments.PaymentResultResponse;
import dev.tuiop.orderservice.products.StockReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPurchaseSagaService {


    private final OrderService orderService;

    public OrderResponse purchase(Jwt jwt, PurchaseRequest purchaseRequest){
        UUID stockReservationId = UUID.randomUUID();

        Map<UUID, Integer> quantitiesByProductId =
                orderService.mergeQuantitiesByProductId(purchaseRequest.items());

        CustomerResponse customerResponse = orderService.getMe(jwt);

        StockReservationResponse reservation = null;

        Order order = null;

        PaymentResultResponse payment;

        try{
            reservation = reserveStock(stockReservationId, quantitiesByProductId);
        }



    }
}
