package com.example.propertyservice.repositories;

import com.example.propertyservice.models.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    @EntityGraph(attributePaths = {"features", "images"})
    Page<Property> findAll (Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"features"})
    Optional<Property> findById(Long id);

    @Query("SELECT p FROM Property p WHERE " +
            "(:location IS NULL OR LOWER(p.location) LIKE :location) " +
            "AND (:minPrice IS NULL OR p.pricePerNight >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.pricePerNight <= :maxPrice)")
    Page<Property> searchProperties(@Param("location") String location,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    Pageable pageable);

    @Modifying
    @Query("UPDATE Property p SET p.averageRating = :rating WHERE p.id = :id")
    void updateRating(@Param("id") Long id, @Param("rating") BigDecimal rating);

    Page<Property> findByOwnerId(Long ownerId, Pageable pageable);
}