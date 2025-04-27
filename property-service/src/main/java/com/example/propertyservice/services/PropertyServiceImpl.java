package com.example.propertyservice.services;

import com.example.propertyservice.client.BookingClient;
import com.example.propertyservice.client.UserClient;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.models.PropertyFeature;
import com.example.propertyservice.repositories.PropertyFeatureRepository;
import com.example.propertyservice.repositories.PropertyRepository;
import com.example.propertyservice.util.JwtTokenUtils;
import com.example.propertyservice.util.PropertyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {
    private final PropertyRepository propertyRepository;
    private final PropertyFeatureRepository propertyFeatureRepository;
    private final BookingClient bookingClient;
    private final UserClient userClient;
    private final JwtTokenUtils jwtTokenUtils;

    @Override
    public List<Property> findAll(){
        return propertyRepository.findAll();
    }

    @Override
    public Property getPropertyById(Long id){
        return propertyRepository.findById(id).orElseThrow(()->
                new PropertyException("Property not found"));
    }

    @Override
    @Transactional
    public Property save(Property property, String token) {
        property.setOwnerId(jwtTokenUtils.getUserId(token));

        Boolean userExists = userClient.userExists(property.getOwnerId());

        if (userExists == null || !userExists) {
            throw new PropertyException("User with id " + property.getOwnerId()
                    + " not found.");
        }

        enrichPropertyForSave(property);
        property.setId(null);
        return propertyRepository.save(property);
    }

    private void enrichPropertyForSave(Property property) {
        property.setFeatures(findOrCreatePropertyFeature(property));
        property.setCreatedAt(LocalDateTime.now());
    }

    @Override
    @Transactional
    public Property updatePropertyById(Long id, Property updatedProperty, String token) {
        Property existingProperty = getPropertyById(id);

        updatedProperty.setOwnerId(jwtTokenUtils.getUserId(token));

        Boolean userExists = userClient.userExists(updatedProperty.getOwnerId());

        if (userExists == null || !userExists) {
            throw new PropertyException("User with id " + updatedProperty.getOwnerId()
                    + " not found.");
        }

        enrichPropertyForUpdate(existingProperty, updatedProperty);

        return propertyRepository.save(existingProperty);
    }

    private void enrichPropertyForUpdate(Property existingProperty, Property updatedProperty) {
        // Обновить основные поля
        existingProperty.setOwnerId(updatedProperty.getOwnerId());
        existingProperty.setTitle(updatedProperty.getTitle());
        existingProperty.setDescription(updatedProperty.getDescription());
        existingProperty.setLocation(updatedProperty.getLocation());
        existingProperty.setPricePerNight(updatedProperty.getPricePerNight());
        existingProperty.setCapacity(updatedProperty.getCapacity());

        Set<PropertyFeature> updatedFeatures = findOrCreatePropertyFeature(updatedProperty);

        existingProperty.getFeatures().clear();
        existingProperty.getFeatures().addAll(updatedFeatures);

        existingProperty.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public Boolean isPropertyAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut) {
        return bookingClient.isAvailable(propertyId, checkIn, checkOut);
    }

    @Override
    public List<LocalDate> getAvailableDates(Long propertyId) {
        return bookingClient.getAvailableDates(propertyId);
    }

    @Override
    public List<Property> search(String location, BigDecimal minPrice, BigDecimal maxPrice) {
        return propertyRepository.searchProperties(location, minPrice, maxPrice);
    }

    private Set<PropertyFeature> findOrCreatePropertyFeature(Property property) {
        return property.getFeatures().stream()
                .map(feature -> propertyFeatureRepository.findByName(feature.getName())
                        .orElseGet(() -> propertyFeatureRepository.save(feature)))
                .collect(Collectors.toSet());
    }

    @Transactional
    public void updateAverageRating(Long propertyId, Double averageRating, Long totalReviews) {
        var property = getPropertyById(propertyId);
        property.setAverageRating(BigDecimal.valueOf(averageRating));
        propertyRepository.save(property);
    }

    @Override
    public Boolean existsById(Long id) {
        return propertyRepository.existsById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        propertyRepository.deleteById(id);
    }
}
