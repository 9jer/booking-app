package com.example.propertyservice.services;

import com.example.propertyservice.dto.ImageDTO;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.repositories.ImageRepository;
import com.example.propertyservice.repositories.PropertyRepository;
import com.example.propertyservice.util.JwtTokenUtils;
import com.example.propertyservice.util.PropertyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @InjectMocks
    private ImageServiceImpl imageService;

    @Test
    void uploadImages_Success() {
        Long propertyId = 1L;
        Long userId = 100L;
        String token = "valid_token";

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.jpg",
                "image/jpeg",
                "some-image-content".getBytes()
        );

        Property property = new Property();
        property.setId(propertyId);
        property.setOwnerId(userId);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(jwtTokenUtils.getUserId(token)).thenReturn(userId);

        List<ImageDTO> result = imageService.uploadImages(propertyId, List.of(file), token);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(imageRepository, times(1)).save(any());
    }

    @Test
    void uploadImages_AccessDenied_ThrowsException() {
        Long propertyId = 1L;
        Long ownerId = 100L;
        Long otherUserId = 200L;
        String token = "other_token";

        Property property = new Property();
        property.setId(propertyId);
        property.setOwnerId(ownerId);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(jwtTokenUtils.getUserId(token)).thenReturn(otherUserId);

        assertThrows(PropertyException.class, () ->
                imageService.uploadImages(propertyId, Collections.emptyList(), token)
        );
    }

    @Test
    void uploadImages_PropertyNotFound_ThrowsException() {
        Long propertyId = 999L;
        String token = "any_token";

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.empty());

        assertThrows(PropertyException.class, () ->
                imageService.uploadImages(propertyId, Collections.emptyList(), token)
        );
    }
}