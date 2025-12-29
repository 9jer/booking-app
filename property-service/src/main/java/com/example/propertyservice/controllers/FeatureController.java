package com.example.propertyservice.controllers;

import com.example.propertyservice.dto.PropertyFeatureDTO;
import com.example.propertyservice.repositories.PropertyFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureController {

    private final PropertyFeatureRepository featureRepository;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<PropertyFeatureDTO>> getAllFeatures() {
        List<PropertyFeatureDTO> features = featureRepository.findAll().stream()
                .map(feature -> modelMapper.map(feature, PropertyFeatureDTO.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(features);
    }
}
