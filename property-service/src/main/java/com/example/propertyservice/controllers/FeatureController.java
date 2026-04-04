package com.example.propertyservice.controllers;

import com.example.propertyservice.dto.PropertyFeatureDTO;
import com.example.propertyservice.mapper.PropertyMapper;
import com.example.propertyservice.repositories.PropertyFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureController {

    private final PropertyFeatureRepository featureRepository;
    private final PropertyMapper propertyMapper;

    @GetMapping
    public ResponseEntity<List<PropertyFeatureDTO>> getAllFeatures() {
        List<PropertyFeatureDTO> features = featureRepository.findAll().stream()
                .map(propertyMapper::toPropertyFeatureDTO)
                .toList();
        return ResponseEntity.ok(features);
    }
}
