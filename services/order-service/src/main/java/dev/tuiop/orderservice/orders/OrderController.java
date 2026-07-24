package dev.tuiop.orderservice.orders;


import dev.tuiop.orderservice.orders.dto.OrderResponse;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.external.payments.PaymentMethod;
import dev.tuiop.orderservice.orders.mapper.OrderMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping("/purchase")
    public ResponseEntity<OrderResponse> purchase(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PurchaseRequest request

    ) {
        return ResponseEntity.status(201).body(orderMapper.toOrderResponse(orderService.purchase(jwt, request)));
    }

    @PostMapping("/my-cart/purchase")
    public ResponseEntity<OrderResponse> purchaseMyCart(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PaymentMethod paymentMethod
            ) {
        return ResponseEntity.status(201).body(orderMapper.toOrderResponse(orderService.purchaseMyCart(jwt, paymentMethod)));
    }

    @GetMapping("/me")
    public Page<OrderResponse> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable
    ) {
        return orderService.getMyOrders(jwt, pageable).map(orderMapper::toOrderResponse);
    }
}
