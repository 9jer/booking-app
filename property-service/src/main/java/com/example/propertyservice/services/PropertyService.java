package com.example.propertyservice.services;

import com.example.propertyservice.dto.GetPropertyDTO;
import com.example.propertyservice.models.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PropertyService {
    Page<GetPropertyDTO> findAll(Pageable pageable);
    Page<GetPropertyDTO> getMyProperties(String token, Pageable pageable);
    GetPropertyDTO getPropertyById(Long id);
    GetPropertyDTO save(Property property, String token);
    GetPropertyDTO updatePropertyById(Long id, Property updatedProperty, String token);
    Boolean isPropertyAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut);
    List<LocalDate> getAvailableDates(Long propertyId);
    void toggleFavorite(Long propertyId, String token);
    Page<GetPropertyDTO> getUserFavorites(String token, Pageable pageable);
    Page<GetPropertyDTO> search(String location, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    GetPropertyDTO updateAverageRating(Long propertyId, Double averageRating, Long totalReviews);
    Boolean existsById(Long id);
    void delete(Long id, String token);
}