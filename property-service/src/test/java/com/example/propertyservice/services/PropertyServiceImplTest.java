package com.example.propertyservice.services;

import com.example.propertyservice.client.BookingClient;
import com.example.propertyservice.client.UserClient;
import com.example.propertyservice.dto.AvailableDatesResponse;
import com.example.propertyservice.dto.GetPropertyDTO;
import com.example.propertyservice.mapper.PropertyMapper;
import com.example.propertyservice.models.Favorite;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;

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
    private FavoriteRepository favoriteRepository;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private UserClient userClient;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private PropertyMapper propertyMapper;

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
        when(propertyMapper.toGetPropertyDTO(property)).thenReturn(getPropertyDTO);

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
        when(propertyMapper.toGetPropertyDTO(property)).thenReturn(getPropertyDTO);

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
        when(propertyMapper.toGetPropertyDTO(property)).thenReturn(getPropertyDTO);

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

        when(propertyMapper.toGetPropertyDTO(existingProperty)).thenReturn(getPropertyDTO);

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

        when(propertyMapper.toGetPropertyDTO(any(Property.class))).thenReturn(getPropertyDTO);

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
        when(propertyMapper.toGetPropertyDTO(property)).thenReturn(getPropertyDTO);

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
        when(propertyMapper.toGetPropertyDTO(property)).thenReturn(getPropertyDTO);

        // When
        Page<GetPropertyDTO> result = propertyService.getMyProperties("token", pageable);

        // Then
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAvailableDates_ShouldReturnDatesFromBookingClient() {
        // Given
        Long propertyId = 1L;
        List<LocalDate> expectedDates = List.of(LocalDate.now(), LocalDate.now().plusDays(1));

        AvailableDatesResponse mockResponse = new AvailableDatesResponse(expectedDates);

        when(bookingClient.getAvailableDates(propertyId)).thenReturn(mockResponse);

        // When
        List<LocalDate> actualDates = propertyService.getAvailableDates(propertyId);

        // Then
        assertEquals(expectedDates, actualDates);
        verify(bookingClient, times(1)).getAvailableDates(propertyId);
    }

    @Test
    void getAvailableDates_WhenBookingServiceIsDown_ShouldReturnEmptyList() {
        // Given
        Long propertyId = 1L;

        AvailableDatesResponse emptyResponse = new AvailableDatesResponse(java.util.Collections.emptyList());

        when(bookingClient.getAvailableDates(propertyId)).thenReturn(emptyResponse);

        // When
        List<LocalDate> actualDates = propertyService.getAvailableDates(propertyId);

        // Then
        assertTrue(actualDates.isEmpty());
        verify(bookingClient, times(1)).getAvailableDates(propertyId);
    }

    @Test
    void getUserFavorites_ShouldReturnPagedFavorites() {
        // Given
        String token = "Bearer test.token";
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Property property = new Property();
        property.setId(100L);
        property.setTitle("My Favorite Villa");
        property.setPricePerNight(java.math.BigDecimal.valueOf(100));
        property.setLocation("Sydney");

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProperty(property);

        GetPropertyDTO expectedDto = new GetPropertyDTO();
        expectedDto.setId(100L);
        expectedDto.setTitle("My Favorite Villa");

        Page<Favorite> favoritePage = new org.springframework.data.domain.PageImpl<>(List.of(favorite));

        when(jwtTokenUtils.getUserId(token)).thenReturn(userId);
        when(favoriteRepository.findAllByUserId(userId, pageable)).thenReturn(favoritePage);
        when(propertyMapper.toGetPropertyDTO(property)).thenReturn(expectedDto);

        // When
        Page<GetPropertyDTO> result = propertyService.getUserFavorites(token, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("My Favorite Villa", result.getContent().get(0).getTitle());

        verify(favoriteRepository, times(1)).findAllByUserId(userId, pageable);
        verify(propertyMapper, times(1)).toGetPropertyDTO(property);
    }

    @Test
    void toggleFavorite_WhenNotExists_ShouldSaveNewFavorite() {
        // Given
        Long propertyId = 100L;
        String token = "Bearer test.token";
        Long userId = 1L;
        Property property = new Property();
        property.setId(propertyId);

        when(jwtTokenUtils.getUserId(token)).thenReturn(userId);
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(favoriteRepository.findByUserIdAndPropertyId(userId, propertyId)).thenReturn(Optional.empty());

        // When
        propertyService.toggleFavorite(propertyId, token);

        // Then
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
        verify(favoriteRepository, never()).delete(any(Favorite.class));
    }

    @Test
    void toggleFavorite_WhenExists_ShouldDeleteFavorite() {
        // Given
        Long propertyId = 100L;
        String token = "Bearer test.token";
        Long userId = 1L;
        Property property = new Property();
        property.setId(propertyId);

        Favorite existingFavorite = new Favorite();
        existingFavorite.setUserId(userId);
        existingFavorite.setProperty(property);

        when(jwtTokenUtils.getUserId(token)).thenReturn(userId);
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(favoriteRepository.findByUserIdAndPropertyId(userId, propertyId)).thenReturn(Optional.of(existingFavorite));

        // When
        propertyService.toggleFavorite(propertyId, token);

        // Then
        verify(favoriteRepository, times(1)).delete(existingFavorite);
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }
}