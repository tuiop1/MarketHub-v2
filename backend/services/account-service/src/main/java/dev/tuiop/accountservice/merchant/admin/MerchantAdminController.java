package dev.tuiop.accountservice.merchant.admin;

import dev.tuiop.accountservice.merchant.dto.MerchantResponse;
import dev.tuiop.accountservice.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MerchantAdminController {

    private final MerchantAdminService merchantAdminService;
    private final MerchantMapper merchantMapper;



    @GetMapping("/unverified")
    public ResponseEntity<Page<MerchantResponse>> getUnverifiedMerchants(Pageable pageable) {
        Page<MerchantResponse> merchants = merchantAdminService.getUnverifiedMerchants(pageable)
                .map(merchantMapper::toResponse);

        return ResponseEntity.ok(merchants);
    }

    @PatchMapping("/{merchantId}/verify")
    public ResponseEntity<Void> verifyMerchant(@PathVariable UUID merchantId) {
        merchantAdminService.verifyMerchant(merchantId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{merchantId}/reject")
    public ResponseEntity<Void> rejectMerchant(@PathVariable UUID merchantId) {
        merchantAdminService.rejectMerchant(merchantId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{merchantId}/disable")
    public ResponseEntity<Void> suspendMerchant(@PathVariable UUID merchantId) {
        merchantAdminService.suspendMerchant(merchantId);

        return ResponseEntity.noContent().build();
    }

}
