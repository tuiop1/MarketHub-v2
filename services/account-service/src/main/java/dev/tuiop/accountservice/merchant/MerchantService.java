package dev.tuiop.accountservice.merchant;

import dev.tuiop.accountservice.customer.CustomerRepository;
import dev.tuiop.accountservice.merchant.dto.MerchantRegistrationRequest;
import dev.tuiop.accountservice.common.exceptions.EmailAlreadyTakenException;
import dev.tuiop.accountservice.merchant.exceptions.MerchantAlreadyExistsException;
import dev.tuiop.accountservice.merchant.exceptions.MerchantShopNameTakenException;
import dev.tuiop.accountservice.merchant.mapper.MerchantMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final CustomerRepository customerRepository;
    private final MerchantMapper merchantMapper;


    @Transactional(readOnly = true)
    public Merchant getMe(Jwt principal){
       Merchant merchant  = merchantRepository.findByKeycloakUserId(principal.getSubject()).orElseThrow(() ->
               new EntityNotFoundException("Merchant not found for keycloakUserId " + principal.getSubject()));

       return merchant;
    }

    @Transactional(readOnly = true)
    public Page<Merchant> getAllActiveAndVerifiedMerchants(Pageable pageable){


       return merchantRepository.findByStatus(MerchantStatus.VERIFIED, pageable);
    }




    @Transactional
    public Merchant create(String keycloakUserId, MerchantRegistrationRequest request) {
        String email = request.email().trim().toLowerCase();
        String shopName = request.shopName().trim();

        if (merchantRepository.existsByEmailIgnoreCase(email) ||
            customerRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyTakenException(email);
        }

        if (merchantRepository.existsByShopNameIgnoreCase(shopName)) {
            throw new MerchantShopNameTakenException(request.shopName());
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
