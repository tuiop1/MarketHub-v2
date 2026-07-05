package dev.tuiop.accountservice.merchant;

import dev.tuiop.accountservice.merchant.dto.MerchantResponse;
import dev.tuiop.accountservice.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/merchants")
@RestController
@RequiredArgsConstructor

public class MerchantController {


    private final MerchantService merchantService;
    private final MerchantMapper merchantMapper;


    @GetMapping
    public ResponseEntity<Page<MerchantResponse>> getAllActiveAndVerifiedMerchants(Pageable pageable) {
        Page<MerchantResponse> merchants = merchantService.getAllActiveAndVerifiedMerchants(pageable)
                .map(merchantMapper::toResponse);

        return ResponseEntity.ok(merchants);
    }




    @PreAuthorize("hasAnyRole('MERCHANT', 'MERCHANT_PENDING', 'MERCHANT_REJECTED')")
    @GetMapping("/me")
    public ResponseEntity<MerchantResponse> getMyMerchant(
            @AuthenticationPrincipal Jwt principal
    ) {
        return ResponseEntity.ok(merchantMapper.toResponse(merchantService.getMe(principal)));

    }

}
