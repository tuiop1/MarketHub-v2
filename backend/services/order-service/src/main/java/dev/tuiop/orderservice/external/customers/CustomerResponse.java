package dev.tuiop.orderservice.external.customers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String email,
        AddressResponse address,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
