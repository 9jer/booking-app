package com.example.propertyservice.controllers;

import com.example.propertyservice.dto.GetPropertyDTO;
import com.example.propertyservice.dto.PropertyDTO;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.services.PropertyService;
import com.example.propertyservice.util.JwtTokenUtils;
import com.example.propertyservice.util.PropertyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PropertyController.class)
@AutoConfigureMockMvc(addFilters = false)
class PropertyControllerIT {

    private static final String ROOT_ENDPOINT = "/api/v1/properties";
    private static final String ID_ENDPOINT = ROOT_ENDPOINT + "/{id}";
    private static final String SEARCH_ENDPOINT = ROOT_ENDPOINT + "/search";
    private static final String AVAILABILITY_ENDPOINT = ID_ENDPOINT + "/availability";
    private static final String AVAILABLE_DATES_ENDPOINT = ID_ENDPOINT + "/available-dates";
    private static final String EXISTS_ENDPOINT = ID_ENDPOINT + "/exists";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PropertyService propertyService;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    @MockBean
    private ModelMapper modelMapper;

    private Property testProperty;
    private PropertyDTO testPropertyDTO;
    private GetPropertyDTO testGetPropertyDTO;
    private String validToken = "valid.token.here";

    @BeforeEach
    void setUp() {
        testProperty = new Property();
        testProperty.setId(1L);
        testProperty.setOwnerId(1L);
        testProperty.setTitle("Test Property");
        testProperty.setDescription("Test Description");
        testProperty.setLocation("Test Location");
        testProperty.setPricePerNight(BigDecimal.valueOf(100));
        testProperty.setCapacity(4);
        testProperty.setCreatedAt(LocalDateTime.now());

        testPropertyDTO = new PropertyDTO();
        testPropertyDTO.setTitle("Test Property");
        testPropertyDTO.setDescription("Test Description");
        testPropertyDTO.setLocation("Test Location");
        testPropertyDTO.setPricePerNight(BigDecimal.valueOf(100));
        testPropertyDTO.setCapacity(4);

        testGetPropertyDTO = new GetPropertyDTO();
        testGetPropertyDTO.setId(1L);
        testGetPropertyDTO.setTitle("Test Property");
        testGetPropertyDTO.setDescription("Test Description");
        testGetPropertyDTO.setLocation("Test Location");
        testGetPropertyDTO.setPricePerNight(BigDecimal.valueOf(100));
        testGetPropertyDTO.setCapacity(4);

        Mockito.when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);
        Mockito.when(jwtTokenUtils.getRoles(anyString())).thenReturn(List.of("ROLE_OWNER"));
    }

    @Test
    void createProperty_WithValidData_ShouldReturnCreatedPropertyDTO() throws Exception {
        Mockito.when(propertyService.save(any(Property.class), anyString()))
                .thenReturn(testGetPropertyDTO);
        Mockito.when(modelMapper.map(any(PropertyDTO.class), eq(Property.class))).thenReturn(testProperty);

        mockMvc.perform(post(ROOT_ENDPOINT)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPropertyDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testGetPropertyDTO.getId()))
                .andExpect(jsonPath("$.title").value(testGetPropertyDTO.getTitle()));
    }

    @Test
    void getAllProperties_ShouldReturnListOfProperties() throws Exception {
        Mockito.when(propertyService.findAll()).thenReturn(List.of(testGetPropertyDTO));

        mockMvc.perform(get(ROOT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties[0].id").value(testGetPropertyDTO.getId()))
                .andExpect(jsonPath("$.properties[0].title").value(testGetPropertyDTO.getTitle()));
    }

    @Test
    void getPropertyById_ShouldReturnProperty() throws Exception {
        Mockito.when(propertyService.getPropertyById(anyLong())).thenReturn(testGetPropertyDTO);

        mockMvc.perform(get(ID_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testGetPropertyDTO.getId()))
                .andExpect(jsonPath("$.title").value(testGetPropertyDTO.getTitle()));
    }

    @Test
    void updateProperty_WithValidData_ShouldReturnUpdatedPropertyDTO() throws Exception {
        Mockito.when(propertyService.updatePropertyById(anyLong(), any(Property.class), anyString()))
                .thenReturn(testGetPropertyDTO);

        Mockito.when(modelMapper.map(any(PropertyDTO.class), eq(Property.class))).thenReturn(testProperty);

        mockMvc.perform(patch(ID_ENDPOINT, 1L)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPropertyDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testGetPropertyDTO.getId()));
    }

    @Test
    void deleteProperty_ShouldReturnOkStatus() throws Exception {
        mockMvc.perform(delete(ID_ENDPOINT, 1L)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void searchProperties_ShouldReturnFilteredProperties() throws Exception {
        Mockito.when(propertyService.search(any(), any(), any()))
                .thenReturn(List.of(testGetPropertyDTO));

        mockMvc.perform(get(SEARCH_ENDPOINT)
                        .param("location", "Test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties[0].id").value(testGetPropertyDTO.getId()));
    }

    @Test
    void checkAvailability_ShouldReturnBoolean() throws Exception {
        Mockito.when(propertyService.isPropertyAvailable(anyLong(), any(), any()))
                .thenReturn(true);

        mockMvc.perform(get(AVAILABILITY_ENDPOINT, 1L)
                        .param("checkIn", "2023-01-01")
                        .param("checkOut", "2023-01-10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getAvailableDates_ShouldReturnListOfDates() throws Exception {
        Mockito.when(propertyService.getAvailableDates(anyLong()))
                .thenReturn(List.of(LocalDate.now().plusDays(1)));

        mockMvc.perform(get(AVAILABLE_DATES_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableDates").isArray());
    }

    @Test
    void propertyExists_ShouldReturnBoolean() throws Exception {
        Mockito.when(propertyService.existsById(anyLong())).thenReturn(true);

        mockMvc.perform(get(EXISTS_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getPropertyById_WhenNotFound_ShouldReturnNotFound() throws Exception {
        Mockito.when(propertyService.getPropertyById(anyLong()))
                .thenThrow(new PropertyException("Property not found"));

        mockMvc.perform(get(ID_ENDPOINT, 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Property not found"));
    }

    @Test
    void createProperty_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        PropertyDTO invalidProperty = new PropertyDTO();
        invalidProperty.setTitle("");

        mockMvc.perform(post(ROOT_ENDPOINT)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProperty)))
                .andExpect(status().isBadRequest());
    }
}