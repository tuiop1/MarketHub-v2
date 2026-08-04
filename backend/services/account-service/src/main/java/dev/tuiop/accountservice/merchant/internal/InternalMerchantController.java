package dev.tuiop.accountservice.merchant.internal;

import dev.tuiop.accountservice.merchant.MerchantService;
import dev.tuiop.accountservice.merchant.dto.MerchantResponse;
import dev.tuiop.accountservice.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/accounts/merchants")
@RequiredArgsConstructor
public class InternalMerchantController {

    private final MerchantService merchantService;
    private final MerchantMapper merchantMapper;

    @GetMapping("/batch")
    public ResponseEntity<List<MerchantResponse>> getMerchantsByIds(
            @RequestParam Collection<UUID> merchantIds
    ) {
        List<MerchantResponse> merchants = merchantService.getByIds(merchantIds)
                .stream()
                .map(merchantMapper::toResponse)
                .toList();

        return ResponseEntity.ok(merchants);
    }

    @GetMapping("/keycloak/{keycloakUserId}")
    public ResponseEntity<MerchantResponse> getMerchantByKeycloakUserId(
            @PathVariable String keycloakUserId
    ) {
        return ResponseEntity.ok(merchantMapper.toResponse(
                merchantService.getByKeycloakUserId(keycloakUserId)
        ));
    }
}
