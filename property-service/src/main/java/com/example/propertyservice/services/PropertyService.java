package com.example.propertyservice.services;

import com.example.propertyservice.client.BookingClient;
import com.example.propertyservice.client.UserClient;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.models.PropertyFeature;
import com.example.propertyservice.repositories.PropertyFeatureRepository;
import com.example.propertyservice.repositories.PropertyRepository;
import com.example.propertyservice.util.PropertyException;
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
public class PropertyService {
    private final PropertyRepository propertyRepository;
    private final PropertyFeatureRepository propertyFeatureRepository;
    private final BookingClient bookingClient;
    private final UserClient userClient;

    public PropertyService(PropertyRepository propertyRepository, PropertyFeatureRepository propertyFeatureRepository, BookingClient bookingClient, UserClient userClient) {
        this.propertyRepository = propertyRepository;
        this.propertyFeatureRepository = propertyFeatureRepository;
        this.bookingClient = bookingClient;
        this.userClient = userClient;
    }

    public List<Property> findAll(){
        return propertyRepository.findAll();
    }

    public Property getPropertyById(Long id){
        return propertyRepository.findById(id).orElseThrow(()->
                new PropertyException("Property not found"));
    }

    @Transactional
    public void save(Property property, String jwtToken) {
        Boolean userExists = userClient.checkUserExists(property.getOwnerId(), jwtToken).block();

        if (userExists == null || !userExists) {
            throw new PropertyException("User with id " + property.getOwnerId()
                    + " not found.");
        }

        enrichPropertyForSave(property);
        property.setId(null);
        propertyRepository.save(property);
    }

    private void enrichPropertyForSave(Property property) {
        property.setFeatures(findOrCreatePropertyFeature(property));
        property.setCreatedAt(LocalDateTime.now());
    }

    @Transactional
    public void updatePropertyById(Long id, Property updatedProperty, String jwtToken) {
        Property existingProperty = getPropertyById(id);

        Boolean userExists = userClient.checkUserExists(updatedProperty.getOwnerId(), jwtToken).block();

        if (userExists == null || !userExists) {
            throw new PropertyException("User with id " + updatedProperty.getOwnerId()
                    + " not found.");
        }

        enrichPropertyForUpdate(existingProperty, updatedProperty);

        propertyRepository.save(existingProperty);
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

    public Boolean isPropertyAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut, String jwtToken) {
        return bookingClient.isPropertyAvailable(propertyId, checkIn, checkOut, jwtToken);
    }

    public List<LocalDate> getAvailableDates(Long propertyId, String jwtToken) {
        return bookingClient.getAvailableDates(propertyId, jwtToken);
    }

    public List<Property> search(String location, BigDecimal minPrice, BigDecimal maxPrice) {
        return propertyRepository.searchProperties(location, minPrice, maxPrice);
    }

    private Set<PropertyFeature> findOrCreatePropertyFeature(Property property) {
        return property.getFeatures().stream()
                .map(feature -> propertyFeatureRepository.findByName(feature.getName())
                        .orElseGet(() -> propertyFeatureRepository.save(feature)))
                .collect(Collectors.toSet());
    }

    public boolean existsById(Long id) {
        return propertyRepository.existsById(id);
    }

    @Transactional
    public void delete(Long id) {
        propertyRepository.deleteById(id);
    }
}
