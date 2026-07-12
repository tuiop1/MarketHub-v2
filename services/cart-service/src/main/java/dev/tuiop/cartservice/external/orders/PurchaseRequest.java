package dev.tuiop.cartservice.external.orders;

import java.util.List;

public record PurchaseRequest(
        List<PurchaseItemRequest> items
) {
}
