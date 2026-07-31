package dev.tuiop.commonevents;

import java.time.Instant;
import java.util.UUID;

public record CustomerRegisteredEvent(
        UUID customerId,
        String email,
        String firstName,
        Instant occurredAt
) {
}
