package dev.tuiop.orderservice.orders;


import com.tuiop.markethub.carts.cart.Cart;
import com.tuiop.markethub.carts.cart.CartRepository;
import com.tuiop.markethub.carts.cart.item.CartItem;
import com.tuiop.markethub.carts.exceptions.EmptyCartException;
import com.tuiop.markethub.common.exceptions.ResourceNotFoundException;
import com.tuiop.markethub.orders.dto.OrderResponse;
import com.tuiop.markethub.orders.dto.PurchaseItemRequest;
import com.tuiop.markethub.orders.dto.PurchaseRequest;
import com.tuiop.markethub.orders.enums.OrderStatus;
import com.tuiop.markethub.orders.enums.PaymentStatus;
import com.tuiop.markethub.orders.mapper.OrderMapper;
import com.tuiop.markethub.products.Product;
import com.tuiop.markethub.products.ProductRepository;
import com.tuiop.markethub.security.user.CustomUserDetails;
import com.tuiop.markethub.users.User;
import com.tuiop.markethub.users.UserRepository;
import dev.tuiop.orderservice.orders.exceptions.OwnProductPurchaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final CartRepository cartRepository;

    @Transactional
    @CacheEvict(value = "public-product", allEntries = true)
    public OrderResponse purchase(
            CustomUserDetails principal,
            PurchaseRequest request
    ) {
       return orderMapper.toOrderResponse(createOrder(principal, mergeQuantitiesByProductId(request.items())));

    }
    @Transactional
    @CacheEvict(value = "public-product", allEntries = true)
    public OrderResponse purchaseMyCart(
            CustomUserDetails principal
    ) {
        UUID userId = principal.getUserId();

        Cart myCart = cartRepository.findDetailedByUserIdForUpdate(userId).orElseThrow(() -> new ResourceNotFoundException(Cart.class, "user.id",userId));

            if(myCart.getCartItems().isEmpty()){
                throw new EmptyCartException();
            }

        Map<UUID, Integer> quantitiesByProductId = myCart.getCartItems().stream().collect(Collectors.toMap(
                userItem -> userItem.getProduct().getId(),
                CartItem::getQuantity
        ));

        Order order = createOrder(principal, quantitiesByProductId);

        myCart.getCartItems().clear();

        return orderMapper.toOrderResponse(order);

    }

    private Order createOrder(
            CustomUserDetails principal,
            Map<UUID, Integer> quantitiesByProductId
    ) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(User.class, principal.getUserId()));
        //merge quantities to avoid duplicated order items

        log.info(
                "Purchase requested: userId={}, uniqueProductCount={}, totalRequestedItems={}",
                user.getId(),
                quantitiesByProductId.size(),
                quantitiesByProductId.values().stream().mapToInt(Integer::intValue).sum()
        );
        Collection<UUID> productIds = quantitiesByProductId.keySet();

        Map<UUID, Product> productsById = productRepository.findBuyableByIdsForUpdate(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        //check if all requested products can be purchased
        validateAllProductsFound(productIds, productsById);

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.PENDING)
                .totalPriceCents(0L)
                .build();

        for (Map.Entry<UUID, Integer> entry : quantitiesByProductId.entrySet()) {
            Product product = productsById.get(entry.getKey());
            int quantity = entry.getValue();

            validateUserDoesNotBuyOwnProduct(product, principal);

            product.decreaseStock(quantity);

            OrderItem orderItem = OrderItem.fromProduct(product, quantity);
            order.addItem(orderItem);
        }

        order.recalculateTotalPrice();

        Order savedOrder = orderRepository.save(order);
        log.info(
                "Order created: orderId={}, userId={}, itemCount={}, totalPriceCents={}, status={}, paymentStatus={}",
                savedOrder.getId(),
                user.getId(),
                savedOrder.getOrderItems().size(),
                savedOrder.getTotalPriceCents(),
                savedOrder.getStatus(),
                savedOrder.getPaymentStatus()
        );
        return savedOrder;

    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(CustomUserDetails principal, Pageable pageable) {
        return orderRepository.findByUserId(principal.getUserId(), pageable)
                .map(orderMapper::toOrderResponse);
    }

    private Map<UUID, Integer> mergeQuantitiesByProductId(Collection<PurchaseItemRequest> items) {

        Map<UUID, Integer> quantitiesByProductId = new LinkedHashMap<>();

        for (PurchaseItemRequest item : items) {
            UUID id = item.productId();
            Integer quantity = item.quantity();

            if (!quantitiesByProductId.containsKey(id)) {
                quantitiesByProductId.put(id, quantity);
            } else {
                quantitiesByProductId.compute(id, (k, previousQuantity) -> previousQuantity + quantity);
            }

        }

        return quantitiesByProductId;
    }


    private void validateAllProductsFound(
            Collection<UUID> requestedProductIds,
            Map<UUID, Product> productsById
    ) {
        for (UUID requestedProductId : requestedProductIds) {
            if (!productsById.containsKey(requestedProductId)) {
                log.warn(
                        "Purchase failed because product is not buyable or does not exist: productId={}",
                        requestedProductId
                );

                throw new ResourceNotFoundException(Product.class, requestedProductId);
            }
        }
    }


    private void validateUserDoesNotBuyOwnProduct(
            Product product,
            CustomUserDetails principal
    ) {
        if (product.getMerchant().getUser().getId().equals(principal.getUserId())) {
            throw new OwnProductPurchaseException("You cannot purchase your own product");
        }
    }
}