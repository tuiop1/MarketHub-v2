package dev.tuiop.accountservice.merchant;

import dev.tuiop.accountservice.merchant.dto.CreateMerchantRequest;
import dev.tuiop.accountservice.merchant.dto.MerchantRegistrationRequest;
import dev.tuiop.accountservice.merchant.dto.MerchantResponse;
import dev.tuiop.accountservice.merchant.exceptions.MerchantAlreadyExistsException;
import dev.tuiop.accountservice.merchant.exceptions.ShopNameAlreadyTakenException;
import dev.tuiop.accountservice.merchant.mapper.MerchantMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantMapper merchantMapper;

    //merchant will be created unverified, should be verified by admin to be in public access


    @Transactional(readOnly = true)
    public MerchantResponse getMyMerchant(Jwt principal){
       Merchant merchant  = merchantRepository.findByKeycloakUserId(principal.getSubject()).orElseThrow(() ->
               new EntityNotFoundException("Merchant not found for keycloakUserId " + principal.getSubject()));

       return merchantMapper.toResponse(merchant);
    }

    @Transactional(readOnly = true)
    public Page<MerchantResponse> getAllActiveAndVerifiedMerchants(Pageable pageable){


       return merchantRepository.findByStatus(MerchantStatus.VERIFIED, pageable).map(merchantMapper::toResponse);
    }



    //========ADMIN=======


    @Transactional
    public MerchantResponse verifyMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        merchant.verify();
        log.info(
                "Merchant verified by admin: merchantId={}, shopName={}",
                merchant.getId(),
                merchant.getShopName()
        );
        return merchantMapper.toResponse(merchant);
    }

    @Transactional
    public MerchantResponse rejectMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        merchant.reject();
        log.info(
                "Merchant rejected by admin: merchantId={}, shopName={}",
                merchant.getId(),
                merchant.getShopName()
        );
        return merchantMapper.toResponse(merchant);
    }

    @Transactional
    public MerchantResponse suspendMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        merchant.suspend();
        log.warn(
                "Merchant suspended by admin: merchantId={}, shopName={}",
                merchant.getId(),
                merchant.getShopName()
        );
        return merchantMapper.toResponse(merchant);
    }

    @Transactional
    public MerchantResponse enableMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        merchant.enable();
        log.info(
                "Merchant enabled by admin: merchantId={}, shopName={}",
                merchant.getId(),
                merchant.getShopName()
        );
        return merchantMapper.toResponse(merchant);
    }

    @Transactional(readOnly = true)
    public Page<MerchantResponse> getUnverifiedMerchants(Pageable pageable) {
        return merchantRepository.findByStatus(MerchantStatus.PENDING, pageable)
                .map(merchantMapper::toResponse);
    }

    @Transactional
    public Merchant create(String keycloakUserId, MerchantRegistrationRequest request) {
        String shopName = request.shopName().trim();

        if (merchantRepository.existsByShopNameIgnoreCase(shopName)) {
            throw new ShopNameAlreadyTakenException(request.shopName());
        }

        if (merchantRepository.existsByKeycloakUserId(keycloakUserId)) {
            throw new MerchantAlreadyExistsException();
        }

        Merchant merchant = merchantMapper.toEntity(keycloakUserId, request);
        Merchant savedMerchant = merchantRepository.save(merchant);

        log.info(
                "Merchant profile created during registration: merchantId={}, keycloakUserId={}, shopName={}",
                savedMerchant.getId(),
                keycloakUserId,
                savedMerchant.getShopName()
        );

        return savedMerchant;
    }
}
