package dev.tuiop.accountservice.merchant;

import dev.tuiop.accountservice.customer.CustomerRepository;
import dev.tuiop.accountservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.accountservice.merchant.dto.MerchantRegistrationRequest;
import dev.tuiop.accountservice.common.exceptions.EmailAlreadyTakenException;
import dev.tuiop.accountservice.merchant.exceptions.MerchantAlreadyExistsException;
import dev.tuiop.accountservice.merchant.exceptions.MerchantShopNameTakenException;
import dev.tuiop.accountservice.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final CustomerRepository customerRepository;
    private final MerchantMapper merchantMapper;

    @Transactional(readOnly = true)
    public Merchant getMe(Jwt jwt) {
        return getByKeycloakUserId(jwt.getSubject());
    }

    @Transactional(readOnly = true)
    public Merchant getById(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException(Merchant.class, merchantId));
    }

    @Transactional(readOnly = true)
    public Merchant getByKeycloakUserId(String keycloakUserId) {
        return merchantRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Merchant.class,
                        "keycloakUserId",
                        keycloakUserId
                ));
    }

    @Transactional(readOnly = true)
    public List<Merchant> getByIds(Collection<UUID> merchantIds) {
        return merchantRepository.findByIdIn(merchantIds);
    }

    @Transactional(readOnly = true)
    public Page<Merchant> getAllActiveAndVerifiedMerchants(Pageable pageable) {
        return merchantRepository.findByStatus(MerchantStatus.VERIFIED, pageable);
    }

    @Transactional
    public Merchant create(String keycloakUserId, String email, MerchantRegistrationRequest request) {
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

        Merchant merchant = merchantMapper.toEntity(keycloakUserId, email, request);
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
