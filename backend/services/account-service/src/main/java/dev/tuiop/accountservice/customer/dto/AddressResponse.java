package dev.tuiop.accountservice.customer.dto;

public record AddressResponse(
        String country,
        String city,
        String street,
        String postalCode,
        String apartment
) {
}
