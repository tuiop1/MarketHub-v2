package dev.tuiop.cartservice.carts;


import dev.tuiop.cartservice.carts.dto.AddToCartRequest;
import dev.tuiop.cartservice.carts.dto.CartItemResponse;
import dev.tuiop.cartservice.carts.dto.CartResponse;
import dev.tuiop.cartservice.carts.orders.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/v1/cart")
@RestController
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;


    @GetMapping
    public ResponseEntity<CartResponse> getMyCart(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(cartService.getMyCart(jwt));
    }


    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItemToCart(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddToCartRequest addToCartRequest
            ) {
        return ResponseEntity.status(HttpStatus.OK).body(cartService.addProductToMyCart(addToCartRequest, jwt));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItemFromCart(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
            ) {
        cartService.removeCartItemFromMyCart(id, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/purchase")
    public ResponseEntity<OrderResponse> purchaseMyCart(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.purchaseMyCart(jwt));
    }


}
