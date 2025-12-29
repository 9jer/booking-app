package com.example.propertyservice.services;

import com.example.propertyservice.client.BookingClient;
import com.example.propertyservice.client.UserClient;
import com.example.propertyservice.dto.GetPropertyDTO;
import com.example.propertyservice.dto.PropertyFeatureDTO;
import com.example.propertyservice.models.Favorite;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.models.PropertyFeature;
import com.example.propertyservice.repositories.FavoriteRepository;
import com.example.propertyservice.repositories.PropertyFeatureRepository;
import com.example.propertyservice.repositories.PropertyRepository;
import com.example.propertyservice.util.JwtTokenUtils;
import com.example.propertyservice.util.PropertyException;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service("propertyServiceImpl")
@Transactional(readOnly = true)
public class PropertyServiceImpl implements PropertyService {
    private final PropertyRepository propertyRepository;
    private final PropertyFeatureRepository propertyFeatureRepository;
    private final FavoriteRepository favoriteRepository;
    private final BookingClient bookingClient;
    private final UserClient userClient;
    private final JwtTokenUtils jwtTokenUtils;
    private final ModelMapper modelMapper;
    private final TransactionTemplate transactionTemplate;

    public PropertyServiceImpl(PropertyRepository propertyRepository,
                               PropertyFeatureRepository propertyFeatureRepository,
                               FavoriteRepository favoriteRepository,
                               BookingClient bookingClient,
                               UserClient userClient,
                               JwtTokenUtils jwtTokenUtils,
                               ModelMapper modelMapper,
                               PlatformTransactionManager transactionManager) {
        this.propertyRepository = propertyRepository;
        this.propertyFeatureRepository = propertyFeatureRepository;
        this.favoriteRepository = favoriteRepository;
        this.bookingClient = bookingClient;
        this.userClient = userClient;
        this.jwtTokenUtils = jwtTokenUtils;
        this.modelMapper = modelMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Page<GetPropertyDTO> findAll(Pageable pageable){
        return propertyRepository.findAll(pageable)
                .map(this::convertToGetPropertyDTO);
    }

    @Override
    public Page<GetPropertyDTO> getMyProperties(String token, Pageable pageable) {
        Long ownerId = jwtTokenUtils.getUserId(token);
        return propertyRepository.findByOwnerId(ownerId, pageable)
                .map(property -> modelMapper.map(property, GetPropertyDTO.class));
    }

    @Override
    @Cacheable(value = "property", key = "#id")
    public GetPropertyDTO getPropertyById(Long id){
        Property property = propertyRepository.findById(id).orElseThrow(()->
                new PropertyException("Property not found"));
        return convertToGetPropertyDTO(property);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CachePut(value = "property", key = "#result.id")
    public GetPropertyDTO save(Property property, String token) {
        property.setOwnerId(jwtTokenUtils.getUserId(token));

        Boolean userExists = userClient.userExists(property.getOwnerId());

        if (userExists == null || !userExists) {
            throw new PropertyException("User with id " + property.getOwnerId()
                    + " not found.");
        }

        return transactionTemplate.execute(status -> {
            enrichPropertyForSave(property);
            property.setId(null);
            Property savedProperty = propertyRepository.save(property);
            return convertToGetPropertyDTO(savedProperty);
        });
    }

    private void enrichPropertyForSave(Property property) {
        property.setFeatures(findOrCreatePropertyFeature(property));

        if (property.getImages() != null) {
            for (var image : property.getImages()) {
                image.setProperty(property);
            }
        }

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

        if (updatedProperty.getImages() != null) {
            existingProperty.getImages().clear();
            for (var image : updatedProperty.getImages()) {
                image.setProperty(existingProperty);
                existingProperty.getImages().add(image);
            }
        }

        existingProperty.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Boolean isPropertyAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut) {
        return bookingClient.isAvailable(propertyId, checkIn, checkOut);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<LocalDate> getAvailableDates(Long propertyId) {
        return bookingClient.getAvailableDates(propertyId).getAvailableDates();
    }

    @Override
    @Transactional
    public void toggleFavorite(Long propertyId, String token) {
        Long userId = jwtTokenUtils.getUserId(token);
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyException("Property not found"));

        Optional<Favorite> existing = favoriteRepository.findByUserIdAndPropertyId(userId, propertyId);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
        } else {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setProperty(property);
            favoriteRepository.save(favorite);
        }
    }

    @Override
    public Page<GetPropertyDTO> getUserFavorites(String token, Pageable pageable) {
        Long userId = jwtTokenUtils.getUserId(token);
        return favoriteRepository.findAllByUserId(userId, pageable)
                .map(favorite -> convertToGetPropertyDTO(favorite.getProperty()));
    }

    @Override
    public Page<GetPropertyDTO> search(String location, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        if (location != null && !location.isBlank()) {
            location = "%" + location.toLowerCase() + "%";
        } else {
            location = null;
        }
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

        BigDecimal newRating = BigDecimal.valueOf(averageRating);

        propertyRepository.updateRating(propertyId, newRating);

        property.setAverageRating(newRating);

        return convertToGetPropertyDTO(property);
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
                        fdto.setId(f.getId());
                        fdto.setName(f.getName());
                        return fdto;
                    })
                    .collect(Collectors.toSet());
            dto.setFeatures(featureDTOs);
        }

        return dto;
    }
}