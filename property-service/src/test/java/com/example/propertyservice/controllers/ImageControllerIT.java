package com.example.propertyservice.controllers;

import com.example.propertyservice.BaseIntegrationTest;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.repositories.PropertyRepository;
import com.example.propertyservice.util.JwtTokenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ImageControllerIT extends BaseIntegrationTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    private Property savedProperty;
    private final String validToken = "valid.token.here";
    private final Path uploadDir = Paths.get("uploads");
    private final String testFileName = "test-image-serve.jpg";

    @BeforeEach
    void setUp() throws IOException {
        Mockito.when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);

        Property property = new Property();
        property.setOwnerId(1L);
        property.setTitle("Image Test Villa");
        property.setDescription("Testing image uploads");
        property.setLocation("Sydney");
        property.setPricePerNight(BigDecimal.valueOf(100));
        property.setCapacity(2);
        property.setAverageRating(BigDecimal.ZERO);
        property.setCreatedAt(LocalDateTime.now());
        savedProperty = propertyRepository.save(property);

        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        Files.write(uploadDir.resolve(testFileName), "dummy image content".getBytes());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(uploadDir.resolve(testFileName));
    }

    @Test
    void uploadImages_ShouldAcceptMultipartFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "test image content".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/images/{propertyId}", savedProperty.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void serveFile_WhenFileExists_ShouldReturnFile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/images/{filename}", testFileName))
                .andExpect(status().isOk());
    }

    @Test
    void serveFile_WhenFileDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/images/{filename}", "non-existent-file.jpg"))
                .andExpect(status().isNotFound());
    }
}