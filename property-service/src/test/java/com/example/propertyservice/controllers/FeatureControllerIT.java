package com.example.propertyservice.controllers;

import com.example.propertyservice.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeatureControllerIT extends BaseIntegrationTest {

    @Test
    void getAllFeatures_ShouldReturnFeaturesList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/features")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}