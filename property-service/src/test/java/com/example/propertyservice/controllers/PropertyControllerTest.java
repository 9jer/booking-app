package com.example.propertyservice.controllers;

import com.example.propertyservice.dto.*;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.services.PropertyService;
import com.example.propertyservice.util.PropertyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyControllerTest {

    @Mock
    private PropertyService propertyService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private PropertyController propertyController;

    private Property property;
    private PropertyDTO propertyDTO;
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

        propertyDTO = new PropertyDTO();
        propertyDTO.setTitle("Test Property");
        propertyDTO.setDescription("Test Description");
        propertyDTO.setLocation("Test Location");
        propertyDTO.setPricePerNight(BigDecimal.valueOf(100));
        propertyDTO.setCapacity(4);

        getPropertyDTO = new GetPropertyDTO();
        getPropertyDTO.setId(1L);
        getPropertyDTO.setOwnerId(1L);
        getPropertyDTO.setTitle("Test Property");
        getPropertyDTO.setDescription("Test Description");
        getPropertyDTO.setLocation("Test Location");
        getPropertyDTO.setPricePerNight(BigDecimal.valueOf(100));
        getPropertyDTO.setCapacity(4);
    }

    @Test
    void createProperty_ValidRequest_ReturnsProperty() {
        // Given
        String authHeader = "Bearer token";
        when(bindingResult.hasErrors()).thenReturn(false);
        when(modelMapper.map(any(PropertyDTO.class), eq(Property.class))).thenReturn(property);
        when(propertyService.save(any(Property.class), anyString())).thenReturn(property);

        // When
        ResponseEntity<Property> response = propertyController.createProperty(authHeader, propertyDTO, bindingResult);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(property, response.getBody());

        verify(propertyService, times(1)).save(any(Property.class), anyString());
        verify(modelMapper, times(1)).map(any(PropertyDTO.class), eq(Property.class));
    }

    @Test
    void createProperty_InvalidRequest_ThrowsPropertyException() {
        // Given
        String authHeader = "Bearer token";
        when(bindingResult.hasErrors()).thenReturn(true);
        //doThrow(new PropertyException("Validation error")).when(bindingResult).getAllErrors();

        // When & Then
        PropertyException exception = assertThrows(PropertyException.class,
                () -> propertyController.createProperty(authHeader, propertyDTO, bindingResult));

        //assertEquals("Validation error", exception.getMessage());
        verify(bindingResult, times(1)).hasErrors();
        verify(propertyService, never()).save(any(Property.class), anyString());
    }

    @Test
    void updateProperty_ValidRequest_ReturnsUpdatedProperty() {
        // Given
        String authHeader = "Bearer token";
        when(bindingResult.hasErrors()).thenReturn(false);
        when(modelMapper.map(any(PropertyDTO.class), eq(Property.class))).thenReturn(property);
        when(propertyService.updatePropertyById(anyLong(), any(Property.class), anyString())).thenReturn(property);

        // When
        ResponseEntity<Property> response = propertyController.updateProperty(authHeader, 1L, propertyDTO, bindingResult);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(property, response.getBody());

        verify(propertyService, times(1)).updatePropertyById(anyLong(), any(Property.class), anyString());
        verify(modelMapper, times(1)).map(any(PropertyDTO.class), eq(Property.class));
    }

    @Test
    void deleteProperty_ValidId_ReturnsOkStatus() {
        // Given
        doNothing().when(propertyService).delete(anyLong());

        // When
        ResponseEntity<HttpStatus> response = propertyController.deleteProperty(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(propertyService, times(1)).delete(1L);
    }

    @Test
    void getAllProperties_ReturnsPropertiesResponse() {
        // Given
        when(propertyService.findAll()).thenReturn(Collections.singletonList(property));
        when(modelMapper.map(any(Property.class), eq(GetPropertyDTO.class))).thenReturn(getPropertyDTO);

        // When
        ResponseEntity<PropertiesResponse> response = propertyController.getAllProperties();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getProperties().size());
        assertEquals(getPropertyDTO, response.getBody().getProperties().get(0));

        verify(propertyService, times(1)).findAll();
        verify(modelMapper, times(1)).map(any(Property.class), eq(GetPropertyDTO.class));
    }

    @Test
    void getPropertyById_PropertyExists_ReturnsGetPropertyDTO() {
        // Given
        when(propertyService.getPropertyById(1L)).thenReturn(property);
        when(modelMapper.map(any(Property.class), eq(GetPropertyDTO.class))).thenReturn(getPropertyDTO);

        // When
        ResponseEntity<GetPropertyDTO> response = propertyController.getPropertyById(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(getPropertyDTO, response.getBody());

        verify(propertyService, times(1)).getPropertyById(1L);
        verify(modelMapper, times(1)).map(any(Property.class), eq(GetPropertyDTO.class));
    }

    @Test
    void searchProperties_WithParameters_ReturnsPropertyList() {
        // Given
        String location = "Test";
        BigDecimal minPrice = BigDecimal.valueOf(50);
        BigDecimal maxPrice = BigDecimal.valueOf(150);
        when(propertyService.search(location, minPrice, maxPrice)).thenReturn(Collections.singletonList(property));

        // When
        ResponseEntity<List<Property>> response = propertyController.searchProperties(location, minPrice, maxPrice);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(property, response.getBody().get(0));

        verify(propertyService, times(1)).search(location, minPrice, maxPrice);
    }

    @Test
    void checkAvailability_PropertyAvailable_ReturnsTrue() {
        // Given
        Long propertyId = 1L;
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        when(propertyService.isPropertyAvailable(propertyId, checkIn, checkOut)).thenReturn(true);

        // When
        ResponseEntity<Boolean> response = propertyController.checkAvailability(propertyId, checkIn, checkOut);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());

        verify(propertyService, times(1)).isPropertyAvailable(propertyId, checkIn, checkOut);
    }

    @Test
    void getAvailableDates_ReturnsAvailableDatesResponse() {
        // Given
        Long propertyId = 1L;
        List<LocalDate> dates = List.of(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        when(propertyService.getAvailableDates(propertyId)).thenReturn(dates);

        // When
        ResponseEntity<AvailableDatesResponse> response = propertyController.getAvailableDates(propertyId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getAvailableDates().size());
        assertEquals(dates, response.getBody().getAvailableDates());

        verify(propertyService, times(1)).getAvailableDates(propertyId);
    }

    @Test
    void propertyExists_PropertyExists_ReturnsTrue() {
        // Given
        when(propertyService.existsById(1L)).thenReturn(true);

        // When
        ResponseEntity<Boolean> response = propertyController.propertyExists(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());

        verify(propertyService, times(1)).existsById(1L);
    }
}