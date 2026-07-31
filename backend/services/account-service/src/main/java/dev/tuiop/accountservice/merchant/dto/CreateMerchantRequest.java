package dev.tuiop.accountservice.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMerchantRequest(
        @NotBlank
        @Size(min = 2, max = 120)
        String shopName,

        @Size(max = 2000)
        String description
) {
}
