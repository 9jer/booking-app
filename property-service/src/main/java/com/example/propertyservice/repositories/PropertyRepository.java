package com.example.propertyservice.repositories;

import com.example.propertyservice.models.Property;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {
    @Query("SELECT p FROM Property p WHERE " +
            "(:location IS NULL OR LOWER(p.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
            "AND (:minPrice IS NULL OR p.pricePerNight >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.pricePerNight <= :maxPrice)")
    List<Property> searchProperties(@Param("location") String location,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice);
}
