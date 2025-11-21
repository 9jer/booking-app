package com.example.propertyservice.services;

import com.example.propertyservice.client.BookingClient;
import com.example.propertyservice.client.UserClient;
import com.example.propertyservice.dto.AvailableDatesResponse;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.models.PropertyFeature;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private BookingClient bookingClient;

    @Mock
    private UserClient userClient;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @InjectMocks
    private PropertyServiceImpl propertyService;

    private Property property;
    private PropertyFeature feature;

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
    }

    @Test
    void findAll_ReturnsListOfProperties() {
        // Given
        when(propertyRepository.findAll()).thenReturn(List.of(property));

        // When
        List<Property> result = propertyService.findAll();

        // Then
        assertEquals(1, result.size());
        assertEquals(property, result.get(0));
        verify(propertyRepository, times(1)).findAll();
    }

    @Test
    void getPropertyById_PropertyExists_ReturnsProperty() {
        // Given
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        // When
        Property result = propertyService.getPropertyById(1L);

        // Then
        assertNotNull(result);
        assertEquals(property.getId(), result.getId());
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
    void save_ValidProperty_CreatesProperty() {
        // Given
        String token = "valid-token";
        property.setId(null);
        property.setFeatures(new HashSet<>(Set.of(feature)));

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(true);
        when(propertyRepository.save(any(Property.class))).thenReturn(property);
        when(propertyFeatureRepository.findByName(anyString())).thenReturn(Optional.of(feature));

        // When
        Property result = propertyService.save(property, token);

        // Then
        assertNotNull(result);
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getFeatures());
        verify(propertyRepository, times(1)).save(property);
        verify(userClient, times(1)).userExists(1L);
        verify(propertyFeatureRepository, atLeastOnce()).findByName(anyString());
    }

    @Test
    @Transactional
    void save_UserNotExists_ThrowsException() {
        // Given
        String token = "valid-token";
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(false);

        // When & Then
        PropertyException exception = assertThrows(PropertyException.class,
                () -> propertyService.save(property, token));

        assertEquals("User with id 1 not found.", exception.getMessage());
        verify(propertyRepository, never()).save(any(Property.class));
    }

    @Test
    @Transactional
    void updatePropertyById_ValidData_UpdatesProperty() {
        // Given
        String token = "valid-token";

        Property existingProperty = new Property();
        existingProperty.setId(1L);
        existingProperty.setOwnerId(1L);
        existingProperty.setFeatures(new HashSet<>());

        Property updatedProperty = new Property();
        updatedProperty.setTitle("Updated Title");
        updatedProperty.setDescription("Updated Description");
        updatedProperty.setFeatures(Set.of(feature));

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existingProperty));

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(true);
        when(propertyRepository.save(any(Property.class))).thenReturn(existingProperty);
        when(propertyFeatureRepository.findByName(anyString())).thenReturn(Optional.of(feature));

        // When
        Property result = propertyService.updatePropertyById(1L, updatedProperty, token);

        // Then
        assertNotNull(result);
        assertEquals("Updated Title", existingProperty.getTitle());
        assertEquals("Updated Description", existingProperty.getDescription());
        assertNotNull(existingProperty.getUpdatedAt());
        assertNotNull(existingProperty.getFeatures());
        assertEquals(1, existingProperty.getFeatures().size());

        verify(propertyRepository, times(1)).save(existingProperty);
        verify(propertyFeatureRepository, atLeastOnce()).findByName(anyString());
    }

    @Test
    void isPropertyAvailable_ReturnsBookingClientResult() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        when(bookingClient.isAvailable(1L, checkIn, checkOut)).thenReturn(true);

        // When
        Boolean result = propertyService.isPropertyAvailable(1L, checkIn, checkOut);

        // Then
        assertTrue(result);
        verify(bookingClient, times(1)).isAvailable(1L, checkIn, checkOut);
    }

    @Test
    void getAvailableDates_ReturnsBookingClientResult() {
        // Given
        List<LocalDate> dates = List.of(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        AvailableDatesResponse response = new AvailableDatesResponse(dates);
        when(bookingClient.getAvailableDates(1L)).thenReturn(response);

        // When
        List<LocalDate> result = propertyService.getAvailableDates(1L);

        // Then
        assertEquals(2, result.size());
        verify(bookingClient, times(1)).getAvailableDates(1L);
    }

    @Test
    void search_ReturnsFilteredProperties() {
        // Given
        String location = "Test";
        BigDecimal minPrice = BigDecimal.valueOf(50);
        BigDecimal maxPrice = BigDecimal.valueOf(150);
        when(propertyRepository.searchProperties(location, minPrice, maxPrice))
                .thenReturn(List.of(property));

        // When
        List<Property> result = propertyService.search(location, minPrice, maxPrice);

        // Then
        assertEquals(1, result.size());
        assertEquals(property, result.get(0));
        verify(propertyRepository, times(1))
                .searchProperties(location, minPrice, maxPrice);
    }

    @Test
    @Transactional
    void updateAverageRating_UpdatesRating() {
        // Given
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenReturn(property);

        // When
        propertyService.updateAverageRating(1L, 4.5, 10L);

        // Then
        assertEquals(BigDecimal.valueOf(4.5), property.getAverageRating());
        verify(propertyRepository, times(1)).save(property);
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
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        doNothing().when(propertyRepository).delete(property);

        // When
        propertyService.delete(1L);

        // Then
        verify(propertyRepository, times(1)).delete(property);
    }

}