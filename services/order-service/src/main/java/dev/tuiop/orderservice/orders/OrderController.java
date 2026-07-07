package dev.tuiop.orderservice.orders;


import com.nimbusds.jwt.JWT;
import dev.tuiop.orderservice.orders.dto.OrderResponse;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse purchase(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PurchaseRequest request
    ) {
        return orderService.purchase(jwt, request);
    }

    @GetMapping("/me")
    public Page<OrderResponse> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable
    ) {
        return orderService.getMyOrders(jwt, pageable);
    }
}