package dev.tuiop.orderservice.orders.dto;

import dev.tuiop.orderservice.external.payments.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PurchaseRequest(
        @NotEmpty
        @Size(max = 50)
        List<@Valid PurchaseItemRequest> items,
        @NotNull

        PaymentMethod paymentMethod
) {


}
