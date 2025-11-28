package com.example.propertyservice.services;

import com.example.propertyservice.client.BookingClient;
import com.example.propertyservice.client.UserClient;
import com.example.propertyservice.dto.GetPropertyDTO;
import com.example.propertyservice.dto.PropertyFeatureDTO;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.models.PropertyFeature;
import com.example.propertyservice.repositories.PropertyFeatureRepository;
import com.example.propertyservice.repositories.PropertyRepository;
import com.example.propertyservice.util.JwtTokenUtils;
import com.example.propertyservice.util.PropertyException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyServiceImpl implements PropertyService {
    private final PropertyRepository propertyRepository;
    private final PropertyFeatureRepository propertyFeatureRepository;
    private final BookingClient bookingClient;
    private final UserClient userClient;
    private final JwtTokenUtils jwtTokenUtils;
    private final ModelMapper modelMapper;

    @Override
    public Page<GetPropertyDTO> findAll(Pageable pageable){
        return propertyRepository.findAll(pageable)
                .map(this::convertToGetPropertyDTO);
    }

    @Override
    @Cacheable(value = "property", key = "#id")
    public GetPropertyDTO getPropertyById(Long id){
        Property property = propertyRepository.findById(id).orElseThrow(()->
                new PropertyException("Property not found"));
        return convertToGetPropertyDTO(property);
    }

    @Override
    @Transactional
    @CachePut(value = "property", key = "#result.id")
    public GetPropertyDTO save(Property property, String token) {
        property.setOwnerId(jwtTokenUtils.getUserId(token));

        Boolean userExists = userClient.userExists(property.getOwnerId());

        if (userExists == null || !userExists) {
            throw new PropertyException("User with id " + property.getOwnerId()
                    + " not found.");
        }

        enrichPropertyForSave(property);
        property.setId(null);
        Property savedProperty = propertyRepository.save(property);

        return convertToGetPropertyDTO(savedProperty);
    }

    private void enrichPropertyForSave(Property property) {
        property.setFeatures(findOrCreatePropertyFeature(property));
        property.setCreatedAt(LocalDateTime.now());
    }

    @Override
    @Transactional
    @CachePut(value = "property", key = "#id")
    public GetPropertyDTO updatePropertyById(Long id, Property updatedProperty, String token) {
        Property existingProperty = propertyRepository.findById(id).orElseThrow(()->
                new PropertyException("Property not found"));

        Long currentUserId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        if (!roles.contains("ROLE_ADMIN") && !existingProperty.getOwnerId().equals(currentUserId)) {
            throw new PropertyException("You can only update your own properties.");
        }

        enrichPropertyForUpdate(existingProperty, updatedProperty);

        Property savedProperty = propertyRepository.save(existingProperty);
        return convertToGetPropertyDTO(savedProperty);
    }

    private void enrichPropertyForUpdate(Property existingProperty, Property updatedProperty) {
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
    public Page<GetPropertyDTO> search(String location, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return propertyRepository.searchProperties(location, minPrice, maxPrice, pageable)
                .map(this::convertToGetPropertyDTO);
    }

    private Set<PropertyFeature> findOrCreatePropertyFeature(Property property) {
        if (property.getFeatures() == null) {
            return new HashSet<>();
        }
        return property.getFeatures().stream()
                .map(feature -> propertyFeatureRepository.findByName(feature.getName())
                        .orElseGet(() -> propertyFeatureRepository.save(feature)))
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    @CachePut(value = "property", key = "#propertyId")
    public GetPropertyDTO updateAverageRating(Long propertyId, Double averageRating, Long totalReviews) {
        var property = propertyRepository.findById(propertyId).orElseThrow(()->
                new PropertyException("Property not found"));

        property.setAverageRating(BigDecimal.valueOf(averageRating));
        Property savedProperty = propertyRepository.save(property);

        return convertToGetPropertyDTO(savedProperty);
    }

    @Override
    public Boolean existsById(Long id) {
        return propertyRepository.existsById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "property", key = "#id")
    public void delete(Long id, String token) {
        Property property = propertyRepository.findById(id).orElseThrow(()->
                new PropertyException("Property not found"));

        Long currentUserId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        if (!roles.contains("ROLE_ADMIN") && !property.getOwnerId().equals(currentUserId)) {
            throw new PropertyException("You can only delete your own properties.");
        }

        propertyRepository.delete(property);
    }

    private GetPropertyDTO convertToGetPropertyDTO(Property property) {
        GetPropertyDTO dto = modelMapper.map(property, GetPropertyDTO.class);

        if (property.getFeatures() != null) {
            Set<PropertyFeatureDTO> featureDTOs = property.getFeatures().stream()
                    .map(f -> {
                        PropertyFeatureDTO fdto = new PropertyFeatureDTO();
                        fdto.setName(f.getName());
                        return fdto;
                    })
                    .collect(Collectors.toSet());
            dto.setFeatures(featureDTOs);
        }

        return dto;
    }
}