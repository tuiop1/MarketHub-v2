package dev.tuiop.cartservice.carts.orders;

import java.util.List;

public record PurchaseRequest(
        List<PurchaseItemRequest> items
) {
}
