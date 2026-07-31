package dev.tuiop.accountservice.customer.dto;

import jakarta.validation.constraints.Size;

public record AddressRequest(
        @Size(max = 100)
        String country,

        @Size(max = 100)
        String city,

        @Size(max = 255)
        String street,

        @Size(max = 32)
        String postalCode,

        @Size(max = 100)
        String apartment
) {
}
