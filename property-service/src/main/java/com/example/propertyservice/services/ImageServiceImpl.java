package com.example.propertyservice.services;

import com.example.propertyservice.dto.ImageDTO;
import com.example.propertyservice.models.Image;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.repositories.ImageRepository;
import com.example.propertyservice.repositories.PropertyRepository;
import com.example.propertyservice.util.JwtTokenUtils;
import com.example.propertyservice.util.PropertyException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final Path storageLocation = Paths.get("/uploads");
    private final PropertyRepository propertyRepository;
    private final ImageRepository imageRepository;
    private final JwtTokenUtils jwtTokenUtils;

    static {
        try {
            Files.createDirectories(Paths.get("/uploads"));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "property", key = "#propertyId")
    public List<ImageDTO> uploadImages(Long propertyId, List<MultipartFile> files, String token) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyException("Property not found with id: " + propertyId));

        Long currentUserId = jwtTokenUtils.getUserId(token);

        if (!property.getOwnerId().equals(currentUserId)) {
            throw new PropertyException("Access Denied: You are not the owner of this property.");
        }

        List<ImageDTO> savedImages = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if (file.isEmpty()) continue;

                String originalFilename = file.getOriginalFilename();
                String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
                Path targetPath = this.storageLocation.resolve(uniqueFilename);
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                Image imageEntity = new Image();
                String imageUrl = "/api/v1/images/" + uniqueFilename;

                imageEntity.setUrl(imageUrl);
                imageEntity.setProperty(property);

                imageRepository.save(imageEntity);

                savedImages.add(new ImageDTO(imageUrl));

            } catch (IOException e) {
                throw new PropertyException("Failed to store file: " + file.getOriginalFilename());
            }
        }
        return savedImages;
    }
}
