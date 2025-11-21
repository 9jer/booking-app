package com.example.propertyservice.services;

import com.example.propertyservice.client.BookingClient;
import com.example.propertyservice.client.UserClient;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.models.PropertyFeature;
import com.example.propertyservice.repositories.PropertyFeatureRepository;
import com.example.propertyservice.repositories.PropertyRepository;
import com.example.propertyservice.util.JwtTokenUtils;
import com.example.propertyservice.util.PropertyException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("propertyServiceImpl")
@Transactional(readOnly = true)
public class PropertyServiceImpl implements PropertyService {
    private final PropertyRepository propertyRepository;
    private final PropertyFeatureRepository propertyFeatureRepository;
    private final BookingClient bookingClient;
    private final UserClient userClient;
    private final JwtTokenUtils jwtTokenUtils;

    public PropertyServiceImpl(PropertyRepository propertyRepository,
                               PropertyFeatureRepository propertyFeatureRepository,
                               BookingClient bookingClient,
                               UserClient userClient,
                               JwtTokenUtils jwtTokenUtils) {
        this.propertyRepository = propertyRepository;
        this.propertyFeatureRepository = propertyFeatureRepository;
        this.bookingClient = bookingClient;
        this.userClient = userClient;
        this.jwtTokenUtils = jwtTokenUtils;
    }

    @Override
    public List<Property> findAll(){
        return propertyRepository.findAll();
    }

    @Override
    @Cacheable(value = "property", key = "#id")
    public Property getPropertyById(Long id){
        Property property = propertyRepository.findById(id).orElseThrow(()->
                new PropertyException("Property not found"));
        return convertToDetachedProperty(property);
    }

    @Override
    @Transactional
    @CachePut(value = "property", key = "#result.id")
    public Property save(Property property, String token) {
        property.setOwnerId(jwtTokenUtils.getUserId(token));

        Boolean userExists = userClient.userExists(property.getOwnerId());

        if (userExists == null || !userExists) {
            throw new PropertyException("User with id " + property.getOwnerId()
                    + " not found.");
        }

        enrichPropertyForSave(property);
        property.setId(null);
        Property savedProperty = propertyRepository.save(property);
        return convertToDetachedProperty(savedProperty);
    }

    private void enrichPropertyForSave(Property property) {
        property.setFeatures(findOrCreatePropertyFeature(property));
        property.setCreatedAt(LocalDateTime.now());
    }

    @Override
    @Transactional
    @CachePut(value = "property", key = "#id")
    public Property updatePropertyById(Long id, Property updatedProperty, String token) {
        Property existingProperty = propertyRepository.findById(id).orElseThrow(()->
                new PropertyException("Property not found"));

        updatedProperty.setOwnerId(jwtTokenUtils.getUserId(token));

        Boolean userExists = userClient.userExists(updatedProperty.getOwnerId());

        if (userExists == null || !userExists) {
            throw new PropertyException("User with id " + updatedProperty.getOwnerId()
                    + " not found.");
        }

        enrichPropertyForUpdate(existingProperty, updatedProperty);

        Property savedProperty = propertyRepository.save(existingProperty);
        return convertToDetachedProperty(savedProperty);
    }

    private void enrichPropertyForUpdate(Property existingProperty, Property updatedProperty) {
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
        return bookingClient.getAvailableDates(propertyId).getAvailableDates();
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
    @CachePut(value = "property", key = "#propertyId")
    public Property updateAverageRating(Long propertyId, Double averageRating, Long totalReviews) {
        var property = propertyRepository.findById(propertyId).orElseThrow(()->
                new PropertyException("Property not found"));
        property.setAverageRating(BigDecimal.valueOf(averageRating));
        Property savedProperty = propertyRepository.save(property);
        return convertToDetachedProperty(savedProperty);
    }

    @Override
    public Boolean existsById(Long id) {
        return propertyRepository.existsById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "property", key = "#id")
    public void delete(Long id) {
        Property property = propertyRepository.findById(id).orElseThrow(()->
                new PropertyException("Property not found"));
        propertyRepository.delete(property);
    }

    private Property convertToDetachedProperty(Property property) {
        Property detachedProperty = new Property();
        detachedProperty.setId(property.getId());
        detachedProperty.setOwnerId(property.getOwnerId());
        detachedProperty.setTitle(property.getTitle());
        detachedProperty.setDescription(property.getDescription());
        detachedProperty.setLocation(property.getLocation());
        detachedProperty.setPricePerNight(property.getPricePerNight());
        detachedProperty.setCapacity(property.getCapacity());
        detachedProperty.setAverageRating(property.getAverageRating());
        detachedProperty.setCreatedAt(property.getCreatedAt());
        detachedProperty.setUpdatedAt(property.getUpdatedAt());

        if (property.getFeatures() != null) {
            detachedProperty.setFeatures(new HashSet<>(property.getFeatures()));
        } else {
            detachedProperty.setFeatures(new HashSet<>());
        }

        return detachedProperty;
    }
}