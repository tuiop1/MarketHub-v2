package dev.tuiop.cartservice.carts;


import dev.tuiop.cartservice.carts.dto.AddToCartRequest;
import dev.tuiop.cartservice.carts.dto.CartItemResponse;
import dev.tuiop.cartservice.carts.dto.CartResponse;
import dev.tuiop.cartservice.carts.exceptions.InactiveCategoryException;
import dev.tuiop.cartservice.carts.exceptions.InactiveProductException;
import dev.tuiop.cartservice.carts.exceptions.InsufficientStockException;
import dev.tuiop.cartservice.carts.exceptions.MerchantNotVerifiedException;
import dev.tuiop.cartservice.carts.exceptions.MerchantPendingVerificationException;
import dev.tuiop.cartservice.carts.exceptions.RejectedMerchantException;
import dev.tuiop.cartservice.carts.exceptions.SuspendedMerchantException;
import dev.tuiop.cartservice.carts.exceptions.UserAlreadyOwnsCartException;
import dev.tuiop.cartservice.carts.item.CartItem;
import dev.tuiop.cartservice.carts.item.CartItemRepository;
import dev.tuiop.cartservice.carts.mapper.CartItemMapper;
import dev.tuiop.cartservice.carts.mapper.CartMapper;

import dev.tuiop.cartservice.categories.CatalogCategoryClient;
import dev.tuiop.cartservice.categories.CategoryResponse;
import dev.tuiop.cartservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.cartservice.customers.AccountCustomerClient;
import dev.tuiop.cartservice.customers.CustomerResponse;
import dev.tuiop.cartservice.customers.exceptions.AccountServiceException;
import dev.tuiop.cartservice.merchants.AccountMerchantClient;
import dev.tuiop.cartservice.merchants.MerchantResponse;
import dev.tuiop.cartservice.products.CatalogProductClient;
import dev.tuiop.cartservice.products.ProductResponse;
import dev.tuiop.cartservice.products.exceptions.CatalogServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final AccountCustomerClient accountCustomerClient;
    private final CatalogProductClient catalogProductClient;
    private final AccountMerchantClient accountMerchantClient;
    private final CatalogCategoryClient catalogCategoryClient;


    private Cart createMyCart(Jwt jwt) {

        CustomerResponse customer = getMe(jwt);




        if (cartRepository.existsByCustomerId(customer.id())) {
            throw new UserAlreadyOwnsCartException();
        }

        Cart newCart = Cart.builder()
                .customerId(customer.id()).build();

        Cart savedCart = cartRepository.save(newCart);


        log.info("Created cart with id={} and the customerId={}", savedCart.getId(), savedCart.getCustomerId());


        return savedCart;
    }

    @Transactional(readOnly = true)
    public CartResponse getMyCart(Jwt jwt) {
        CustomerResponse me = getMe(jwt);

        Cart myCart = cartRepository.findCartByCustomerId(me.id()).orElseThrow(() -> new ResourceNotFoundException(Cart.class, "customerId", me.id()));

        return toCartResponse(myCart);
    }


    // public method for controller
    @Transactional
    public CartItemResponse addProductToMyCart(AddToCartRequest request, Jwt jwt) {

        CartItem cartItem = addProductToCart(request.productId(), request.quantity(), jwt);
        ProductResponse product = getProductById(cartItem.getProductId());

        return cartItemMapper.toCartItemResponse(cartItem, product);
    }

    @Transactional
    public void removeCartItemFromMyCart(UUID id, Jwt jwt) {


        removeCartItemFromCart(id, jwt);

    }

    @Transactional
    public void clearMyCart(Jwt jwt) {
        CustomerResponse customer = getMe(jwt);

        Cart cart = cartRepository.findByCustomerIdForUpdate(customer.id())
                .orElseThrow(() -> new ResourceNotFoundException(Cart.class, "customerId", customer.id()));

        cart.getCartItems().clear();

        log.info("Cleared cart id={} for customerId={}", cart.getId(), customer.id());
    }



    private CartResponse toCartResponse(Cart cart) {
        List<UUID> productIds = new ArrayList<>();
        cart.getCartItems().forEach(cartItem -> productIds.add(cartItem.getProductId()));
        List<ProductResponse> productsReturned = catalogProductClient.getBuyableProductsByIds(productIds);
        Map<UUID, ProductResponse> products = productsReturned.stream().collect(Collectors.toMap((productResponse -> productResponse.id()), Function.identity()));

        List<CartItemResponse> cartItems = cart.getCartItems()
                .stream()
                .map(item -> cartItemMapper.toCartItemResponse(item, products.get(item.getProductId()) ))
                .toList();

        return cartMapper.toCartResponse(cart, cartItems);
    }



    // private method not for controller
    private CartItem addProductToCart(UUID productId, Integer quantity, Jwt jwt) {

        CustomerResponse customer = getMe(jwt);


        ProductResponse product = getProductById(productId);

        CategoryResponse category = getCategoryById(product.categoryId());
        MerchantResponse merchant = getMerchantById(product.merchantId());


        if (!Boolean.TRUE.equals(category.active())) {

            throw new InactiveCategoryException(category.name());
        }

        validateMerchantCanSell(merchant);

        if (!Boolean.TRUE.equals(product.active())) {
            throw new InactiveProductException(product.name());
        }

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity can't be negative");
        }

        if (quantity > product.stockQuantity()) {
            throw new InsufficientStockException(product.name(), quantity, product.stockQuantity());
        }
        Cart myCart = cartRepository.findByCustomerIdForUpdate(customer.id()).orElseGet(() -> createMyCart(jwt));

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductIdForUpdate(myCart.getId(), productId);

        //check if cart already contains order item of this product
        if (existingItem.isPresent()) {


            int requestedQuantity = Math.addExact(existingItem.get().getQuantity(), quantity);
            int availableQuantity = product.stockQuantity();

            if (requestedQuantity > availableQuantity) {
                throw new InsufficientStockException(product.name(), requestedQuantity, availableQuantity);
            }

            existingItem.get().increaseQuantity(quantity);
            return existingItem.get();
        }

        CartItem cartItem = CartItem.builder()
                .cart(myCart)
                .productId(product.id())
                .quantity(quantity)
                .build();


        CartItem savedCartItem = cartItemRepository.save(cartItem);


        log.info("Added cart item id={} to the cart id={}", savedCartItem.getId(), myCart.getId());

        return cartItem;
    }


    private void removeCartItemFromCart(UUID cartItemId, Jwt jwt) {

        CustomerResponse customer = getMe(jwt);


        Cart cart = cartRepository.findByCustomerId(customer.id()).orElseThrow(() -> new ResourceNotFoundException(Cart.class, "customerId", customer.id()));


        CartItem existingItem = cartItemRepository.findByCartIdAndCartItemIdForDelete(cart.getId(), cartItemId).orElseThrow(() -> new ResourceNotFoundException(CartItem.class, cartItemId));


        // item will be implicitly deleted from the cart list as well
        cartItemRepository.delete(existingItem);

        log.info("Deleted cart item id={} from the cart id={}", existingItem.getId(), cart.getId());


    }


    private CustomerResponse getMe(Jwt jwt) {
        try {
            return accountCustomerClient.getMe(authorizationHeader(jwt));
        } catch (HttpClientErrorException.NotFound exception ){
            throw new ResourceNotFoundException(CustomerResponse.class, "current");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Account service authorization failed while getting current customer", exception);
            throw AccountServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Account service request failed while getting current customer", exception);
            throw AccountServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Account service client failed while getting current customer", exception);
            throw AccountServiceException.unavailable(exception);
        }
    }

    private String authorizationHeader(Jwt jwt) {
        return "Bearer " + jwt.getTokenValue();
    }

    private ProductResponse getProductById(UUID productId) {
        try {
            return catalogProductClient.getProductById(productId);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(ProductResponse.class, productId);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Catalog service authorization failed while getting product by id", exception);
            throw CatalogServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Catalog service request failed while getting product by id", exception);
            throw CatalogServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Catalog service client failed while getting product by id", exception);
            throw CatalogServiceException.unavailable(exception);
        }
    }

    private CategoryResponse getCategoryById(UUID categoryId) {
        try {
            return catalogCategoryClient.getCategoryById(categoryId);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(CategoryResponse.class, categoryId);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Catalog service authorization failed while getting category by id", exception);
            throw CatalogServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Catalog service request failed while getting category by id", exception);
            throw CatalogServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Catalog service client failed while getting category by id", exception);
            throw CatalogServiceException.unavailable(exception);
        }
    }

    private MerchantResponse getMerchantById(UUID merchantId) {
        try {
            return accountMerchantClient.getMerchantById(merchantId);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(MerchantResponse.class, merchantId);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Account service authorization failed while getting merchant by id", exception);
            throw AccountServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Account service request failed while getting merchant by id", exception);
            throw AccountServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Account service client failed while getting merchant by id", exception);
            throw AccountServiceException.unavailable(exception);
        }
    }

    private void validateMerchantCanSell(MerchantResponse merchant) {
        if (merchant.status() == null) {
            throw new MerchantNotVerifiedException(merchant.shopName());
        }

        switch (merchant.status()) {
            case VERIFIED -> {
            }
            case PENDING -> throw new MerchantPendingVerificationException(merchant.shopName());
            case REJECTED -> throw new RejectedMerchantException(merchant.shopName());
            case SUSPENDED -> throw new SuspendedMerchantException(merchant.shopName());
        }
    }


}
