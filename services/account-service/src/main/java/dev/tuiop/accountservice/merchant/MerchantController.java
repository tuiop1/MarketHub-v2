package dev.tuiop.accountservice.merchant;

import dev.tuiop.accountservice.merchant.dto.CreateMerchantRequest;
import dev.tuiop.accountservice.merchant.dto.MerchantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/merchants")
@RestController
@RequiredArgsConstructor

public class MerchantController {


    private final MerchantService merchantService;


    @GetMapping
    public ResponseEntity<Page<MerchantResponse>> getAllActiveAndVerifiedMerchants(Pageable pageable) {
        return ResponseEntity.ok(merchantService.getAllActiveAndVerifiedMerchants(pageable));
    }





    @GetMapping("/me")
    public ResponseEntity<MerchantResponse> getMyMerchant(
            @AuthenticationPrincipal Jwt principal
    ) {
        return ResponseEntity.ok(merchantService.getMyMerchant(principal));

    }

}
