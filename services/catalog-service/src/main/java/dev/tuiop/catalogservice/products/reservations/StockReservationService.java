package dev.tuiop.catalogservice.products.reservations;

import dev.tuiop.catalogservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.catalogservice.products.Product;
import dev.tuiop.catalogservice.products.ProductRepository;
import dev.tuiop.catalogservice.products.reservations.dto.StockReservationItemRequest;
import dev.tuiop.catalogservice.products.reservations.dto.StockReservationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReservationService {

    private final StockReservationRepository stockReservationRepository;
    private final ProductRepository productRepository;

    @Transactional
    public StockReservation reserveStock(StockReservationRequest request) {
        if (stockReservationRepository.existsById(request.reservationId())) {
            return stockReservationRepository.findById(request.reservationId()).get();
        }

        Map<UUID, Integer> quantitiesByProductId = mergeAndValidateItems(request.items());

        List<Product> products = productRepository.findAllByIdInForUpdate(quantitiesByProductId.keySet());

        if (products.size() != quantitiesByProductId.size()) {
            throw new ResourceNotFoundException(Product.class, "ids", quantitiesByProductId.keySet());
        }

        Map<UUID, Product> productsById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        StockReservation reservation = StockReservation.builder()
                .id(request.reservationId())
                .build();

        for (UUID id : quantitiesByProductId.keySet()) {
            Product product = productsById.get(id);
            Integer quantity = quantitiesByProductId.get(id);

            product.reserveStock(quantity);
            reservation.addItem(product, quantity);
        }

        StockReservation savedReservation = stockReservationRepository.save(reservation);

        log.info(
                "Stock reserved: reservationId={}, productCount={}",
                savedReservation.getId(),
                savedReservation.getItems().size()
        );

        return savedReservation;
    }

    @Transactional
    public void releaseReservation(UUID reservationId) {
        StockReservation reservation = stockReservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(StockReservation.class, reservationId));

        List<UUID> productIds = reservation.getItems()
                .stream()
                .map(StockReservationItem::getProductId)
                .toList();

        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);

        Map<UUID, Product> productsById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        StockReservationStatus previousStatus = reservation.getStatus();
        reservation.release(productsById);

        if (previousStatus != reservation.getStatus()) {
            log.info(
                    "Stock reservation released: reservationId={}, productCount={}",
                    reservationId,
                    reservation.getItems().size()
            );
        }
    }

    @Transactional
    public void commitReservation(UUID reservationId) {
        StockReservation reservation = stockReservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(StockReservation.class, reservationId));

        StockReservationStatus previousStatus = reservation.getStatus();
        reservation.commit();

        if (previousStatus != reservation.getStatus()) {
            log.info(
                    "Stock reservation committed: reservationId={}, productCount={}",
                    reservationId,
                    reservation.getItems().size()
            );
        }
    }

    private Map<UUID, Integer> mergeAndValidateItems(List<StockReservationItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("List of items to be reversed is not present");
        }

        Map<UUID, Integer> quantitiesByProductId = new LinkedHashMap<>();

        for (var item : items) {
            if (item.productId() == null) {
                throw new IllegalArgumentException("An item to be reserved has no reference to product");
            }

            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity of item to be reserved can't be " + item.quantity());
            }

            quantitiesByProductId.merge(item.productId(), item.quantity(), Integer::sum);
        }

        return quantitiesByProductId;
    }
}
