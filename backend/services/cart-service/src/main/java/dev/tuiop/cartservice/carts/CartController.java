package dev.tuiop.cartservice.carts;


import dev.tuiop.cartservice.carts.dto.AddToCartRequest;
import dev.tuiop.cartservice.carts.dto.CartItemResponse;
import dev.tuiop.cartservice.carts.dto.CartResponse;
import dev.tuiop.cartservice.carts.item.CartItem;
import dev.tuiop.cartservice.carts.mapper.CartItemMapper;
import dev.tuiop.cartservice.carts.mapper.CartMapper;
import dev.tuiop.cartservice.external.products.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequestMapping("/api/v1/carts")
@RestController
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;


    @GetMapping
    public ResponseEntity<CartResponse> getMyCart(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(toCartResponse(cartService.getMyCart(jwt)));
    }


    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItemToCart(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddToCartRequest addToCartRequest
            ) {
        CartItem cartItem = cartService.addProductToMyCart(addToCartRequest, jwt);
        ProductResponse product = cartService.getProductById(cartItem.getProductId());

        return ResponseEntity.status(HttpStatus.OK).body(cartItemMapper.toCartItemResponse(cartItem, product));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItemFromCart(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
            ) {
        cartService.removeCartItemFromMyCart(id, jwt);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> clearMyCart(
            @AuthenticationPrincipal Jwt jwt
    ) {
        cartService.clearMyCart(jwt);
        return ResponseEntity.noContent().build();
    }




    private CartResponse toCartResponse(Cart cart) {
        List<UUID> productIds = cart.getCartItems()
                .stream()
                .map(CartItem::getProductId)
                .toList();
        Map<UUID, ProductResponse> products = cartService.getBuyableProductsByIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductResponse::id, Function.identity()));

        List<CartItemResponse> cartItems = cart.getCartItems()
                .stream()
                .map(item -> cartItemMapper.toCartItemResponse(item, products.get(item.getProductId())))
                .toList();

        return cartMapper.toCartResponse(cart, cartItems);
    }
}
