package com.example.propertyservice.services;

import com.example.propertyservice.dto.ImageDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {
    List<ImageDTO> uploadImages(Long propertyId, List<MultipartFile> files, String token);
}