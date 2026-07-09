package dev.tuiop.cartservice.customers;

public record AddressResponse(
        String country,
        String city,
        String street,
        String postalCode,
        String apartment
) {
}
