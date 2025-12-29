package com.example.propertyservice.controllers;

import com.example.propertyservice.dto.GetPropertyDTO;
import com.example.propertyservice.services.PropertyService;
import com.example.propertyservice.util.JwtTokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyService propertyService;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    private String validToken = "valid.token.here";
    private GetPropertyDTO propertyDTO;

    @BeforeEach
    void setUp() {
        propertyDTO = new GetPropertyDTO();
        propertyDTO.setId(1L);
        propertyDTO.setTitle("Luxury Villa");
    }

    @Test
    void toggleFavorite_ReturnsOk() throws Exception {
        // Given
        Long propertyId = 1L;
        doNothing().when(propertyService).toggleFavorite(eq(propertyId), anyString());

        // When & Then
        mockMvc.perform(post("/api/v1/favorites/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(propertyService, times(1)).toggleFavorite(eq(propertyId), anyString());
    }

    @Test
    void getFavorites_ReturnsListOfProperties() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(propertyService.getUserFavorites(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(propertyDTO)));

        // When & Then
        mockMvc.perform(get("/api/v1/favorites")
                        .header("Authorization", "Bearer " + validToken)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Luxury Villa"));

        verify(propertyService, times(1)).getUserFavorites(anyString(), any(Pageable.class));
    }
}