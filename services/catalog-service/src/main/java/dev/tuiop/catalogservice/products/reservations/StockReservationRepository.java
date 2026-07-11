package dev.tuiop.catalogservice.products.reservations;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select sr
           from StockReservation sr
           join fetch sr.items 
           where sr.id = :id
""")
    Optional<StockReservation> findByIdForUpdate(@Param("id") UUID id);
}
