package com.example.propertyservice.services;

import com.example.propertyservice.models.Property;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PropertyService {

    List<Property> findAll();
    Property getPropertyById(Long id);
    Property save(Property property, String jwtToken);
    Property updatePropertyById(Long id, Property updatedProperty, String jwtToken);
    Boolean isPropertyAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut, String jwtToken);
    List<LocalDate> getAvailableDates(Long propertyId, String jwtToken);
    List<Property> search(String location, BigDecimal minPrice, BigDecimal maxPrice);
    Boolean existsById(Long id);
    void delete(Long id);

}
