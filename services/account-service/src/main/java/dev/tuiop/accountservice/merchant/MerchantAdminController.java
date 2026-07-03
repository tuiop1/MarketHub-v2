package dev.tuiop.accountservice.merchant;

import dev.tuiop.accountservice.merchant.dto.MerchantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
public class MerchantAdminController {

    private final MerchantService merchantService;



    @GetMapping("/unverified")
    public Page<MerchantResponse> getUnverifiedMerchants(Pageable pageable) {
        return merchantService.getUnverifiedMerchants(pageable);
    }

    @PatchMapping("/{merchantId}/verify")
    public MerchantResponse verifyMerchant(@PathVariable UUID merchantId) {
        return merchantService.verifyMerchant(merchantId);
    }

    @PatchMapping("/{merchantId}/reject")
    public MerchantResponse rejectMerchant(@PathVariable UUID merchantId) {
        return merchantService.rejectMerchant(merchantId);
    }

    @PatchMapping("/{merchantId}/disable")
    public MerchantResponse suspendMerchant(@PathVariable UUID merchantId) {
        return merchantService.suspendMerchant(merchantId);
    }

    @PatchMapping("/{merchantId}/enable")
    public MerchantResponse enableMerchant(@PathVariable UUID merchantId) {
        return merchantService.enableMerchant(merchantId);
    }


}
