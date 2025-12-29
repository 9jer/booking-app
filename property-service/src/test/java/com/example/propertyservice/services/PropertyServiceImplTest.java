package com.example.propertyservice.services;

import com.example.propertyservice.client.UserClient;
import com.example.propertyservice.dto.GetPropertyDTO;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.models.PropertyFeature;
import com.example.propertyservice.repositories.FavoriteRepository;
import com.example.propertyservice.repositories.PropertyFeatureRepository;
import com.example.propertyservice.repositories.PropertyRepository;
import com.example.propertyservice.util.JwtTokenUtils;
import com.example.propertyservice.util.PropertyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyServiceImplTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyFeatureRepository propertyFeatureRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private PropertyServiceImpl propertyService;

    private Property property;
    private PropertyFeature feature;
    private GetPropertyDTO getPropertyDTO;

    @BeforeEach
    void setUp() {
        property = new Property();
        property.setId(1L);
        property.setOwnerId(1L);
        property.setTitle("Test Property");
        property.setDescription("Test Description");
        property.setLocation("Test Location");
        property.setPricePerNight(BigDecimal.valueOf(100));
        property.setCapacity(4);
        property.setCreatedAt(LocalDateTime.now());
        property.setFeatures(new HashSet<>());

        feature = new PropertyFeature();
        feature.setId(1L);
        feature.setName("WiFi");

        getPropertyDTO = new GetPropertyDTO();
        getPropertyDTO.setId(1L);
        getPropertyDTO.setTitle("Test Property");

        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void findAll_ReturnsListOfDTOs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(propertyRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(property)));
        when(modelMapper.map(property, GetPropertyDTO.class)).thenReturn(getPropertyDTO);

        // When
        Page<GetPropertyDTO> result = propertyService.findAll(pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(getPropertyDTO.getId(), result.getContent().get(0).getId());
        verify(propertyRepository, times(1)).findAll(pageable);
    }

    @Test
    void getPropertyById_PropertyExists_ReturnsDTO() {
        // Given
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(modelMapper.map(property, GetPropertyDTO.class)).thenReturn(getPropertyDTO);

        // When
        GetPropertyDTO result = propertyService.getPropertyById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(propertyRepository, times(1)).findById(1L);
    }

    @Test
    void getPropertyById_PropertyNotExists_ThrowsException() {
        // Given
        when(propertyRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        PropertyException exception = assertThrows(PropertyException.class,
                () -> propertyService.getPropertyById(1L));

        assertEquals("Property not found", exception.getMessage());
        verify(propertyRepository, times(1)).findById(1L);
    }

    @Test
    @Transactional
    void save_ValidProperty_CreatesPropertyAndReturnsDTO() {
        // Given
        String token = "valid-token";
        property.setId(null);
        property.setFeatures(new HashSet<>(Set.of(feature)));

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(true);
        when(propertyRepository.save(any(Property.class))).thenReturn(property);
        when(propertyFeatureRepository.findByName(anyString())).thenReturn(Optional.of(feature));
        when(modelMapper.map(property, GetPropertyDTO.class)).thenReturn(getPropertyDTO);

        // When
        GetPropertyDTO result = propertyService.save(property, token);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(propertyRepository, times(1)).save(property);
    }

    @Test
    @Transactional
    void updatePropertyById_ValidData_UpdatesPropertyAndReturnsDTO() {
        // Given
        String token = "valid-token";

        Property existingProperty = new Property();
        existingProperty.setId(1L);
        existingProperty.setOwnerId(1L);
        existingProperty.setFeatures(new HashSet<>());

        Property updatedProperty = new Property();
        updatedProperty.setTitle("Updated Title");
        updatedProperty.setFeatures(Set.of(feature));

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existingProperty));
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);

        when(propertyRepository.save(any(Property.class))).thenReturn(existingProperty);
        when(propertyFeatureRepository.findByName(anyString())).thenReturn(Optional.of(feature));

        when(modelMapper.map(existingProperty, GetPropertyDTO.class)).thenReturn(getPropertyDTO);

        // When
        GetPropertyDTO result = propertyService.updatePropertyById(1L, updatedProperty, token);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(propertyRepository, times(1)).save(existingProperty);
    }

    @Test
    void search_ReturnsFilteredDTOs() {
        // Given
        String location = "test";
        String expectedLocationPattern = "%test%";
        BigDecimal minPrice = BigDecimal.valueOf(50);
        BigDecimal maxPrice = BigDecimal.valueOf(150);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Property> propertyPage = new PageImpl<>(List.of(property));

        when(propertyRepository.searchProperties(
                eq(expectedLocationPattern),
                eq(minPrice),
                eq(maxPrice),
                eq(pageable)
        )).thenReturn(propertyPage);

        when(modelMapper.map(any(Property.class), eq(GetPropertyDTO.class))).thenReturn(getPropertyDTO);

        // When
        Page<GetPropertyDTO> result = propertyService.search(location, minPrice, maxPrice, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(propertyRepository, times(1)).searchProperties(
                eq(expectedLocationPattern),
                eq(minPrice),
                eq(maxPrice),
                eq(pageable)
        );
    }

    @Test
    @Transactional
    void updateAverageRating_UpdatesRatingAndReturnsDTO() {
        // Given
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(modelMapper.map(property, GetPropertyDTO.class)).thenReturn(getPropertyDTO);

        // When
        GetPropertyDTO result = propertyService.updateAverageRating(1L, 4.5, 10L);

        // Then
        assertNotNull(result);
        verify(propertyRepository, times(1)).updateRating(eq(1L), eq(BigDecimal.valueOf(4.5)));
    }

    @Test
    void existsById_ReturnsRepositoryResult() {
        // Given
        when(propertyRepository.existsById(1L)).thenReturn(true);

        // When
        Boolean result = propertyService.existsById(1L);

        // Then
        assertTrue(result);
        verify(propertyRepository, times(1)).existsById(1L);
    }

    @Test
    @Transactional
    void delete_DeletesProperty() {
        // Given
        String token = "valid-token";
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        doNothing().when(propertyRepository).delete(property);

        // When
        propertyService.delete(1L, token);

        // Then
        verify(propertyRepository, times(1)).delete(property);
    }

    @Test
    @Transactional
    void toggleFavorite_WhenNotExists_CreatesFavorite() {
        // Given
        when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(favoriteRepository.findByUserIdAndPropertyId(1L, 1L)).thenReturn(Optional.empty());

        // When
        propertyService.toggleFavorite(1L, "token");

        // Then
        verify(favoriteRepository).save(any());
    }

    @Test
    @Transactional
    void toggleFavorite_WhenExists_DeletesFavorite() {
        // Given
        com.example.propertyservice.models.Favorite favorite = new com.example.propertyservice.models.Favorite();
        when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(favoriteRepository.findByUserIdAndPropertyId(1L, 1L)).thenReturn(Optional.of(favorite));

        // When
        propertyService.toggleFavorite(1L, "token");

        // Then
        verify(favoriteRepository).delete(favorite);
    }

    @Test
    void getMyProperties_ReturnsPageOfDTOs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(jwtTokenUtils.getUserId("token")).thenReturn(1L);
        when(propertyRepository.findByOwnerId(1L, pageable)).thenReturn(new PageImpl<>(List.of(property)));
        when(modelMapper.map(property, GetPropertyDTO.class)).thenReturn(getPropertyDTO);

        // When
        Page<GetPropertyDTO> result = propertyService.getMyProperties("token", pageable);

        // Then
        assertEquals(1, result.getTotalElements());
    }
}