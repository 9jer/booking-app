package com.example.propertyservice.services;

import com.example.propertyservice.models.Property;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PropertyService {

    List<Property> findAll();
    Property getPropertyById(Long id);
    Property save(Property property);
    Property updatePropertyById(Long id, Property updatedProperty);
    Boolean isPropertyAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut);
    List<LocalDate> getAvailableDates(Long propertyId);
    List<Property> search(String location, BigDecimal minPrice, BigDecimal maxPrice);
    Boolean existsById(Long id);
    void delete(Long id);

}
