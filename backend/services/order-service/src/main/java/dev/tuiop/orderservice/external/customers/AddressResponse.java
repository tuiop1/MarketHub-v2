package dev.tuiop.orderservice.external.customers;

public record AddressResponse(
        String country,
        String city,
        String street,
        String postalCode,
        String apartment
) {
}
