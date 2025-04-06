package com.example.propertyservice.controllers;

import com.example.propertyservice.dto.AvailableDatesResponse;
import com.example.propertyservice.dto.PropertiesResponse;
import com.example.propertyservice.dto.PropertyDTO;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.services.PropertyService;
import com.example.propertyservice.util.ErrorsUtil;
import com.example.propertyservice.util.PropertyErrorResponse;
import com.example.propertyservice.util.PropertyException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<Property> createProperty(@RequestHeader("Authorization") String authorizationHeader,
                                                     @RequestBody @Valid PropertyDTO propertyDTO,
                                                     BindingResult bindingResult) {
        Property property = convertPropertyDTOToProperty(propertyDTO);

        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        String jwtToken = authorizationHeader.replace("Bearer ", "");
        Property savedProperty = propertyService.save(property, jwtToken);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(savedProperty);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Property> updateProperty(@RequestHeader("Authorization") String authorizationHeader,
                                                     @PathVariable("id") Long id, @RequestBody @Valid PropertyDTO propertyDTO, BindingResult bindingResult) {
        Property property = convertPropertyDTOToProperty(propertyDTO);

        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        String jwtToken = authorizationHeader.replace("Bearer ", "");
        Property updatedProperty = propertyService.updatePropertyById(id, property, jwtToken);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatedProperty);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteProperty(@PathVariable("id") Long id) {
        propertyService.delete(id);

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<PropertiesResponse> getAllProperties() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PropertiesResponse(propertyService.findAll().stream()
                .map(this::convertPropertyToPropertyDTO).collect(Collectors.toList())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyDTO> getPropertyById(@PathVariable("id") Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(convertPropertyToPropertyDTO(propertyService.getPropertyById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Property>> searchProperties(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(propertyService.search(location, minPrice, maxPrice));
    }


    @GetMapping("/{id}/availability")
    public ResponseEntity<Boolean> checkAvailability(@RequestHeader("Authorization") String authorizationHeader,
                                     @PathVariable Long id,
                                     @RequestParam LocalDate checkIn,
                                     @RequestParam LocalDate checkOut) {
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok(propertyService.isPropertyAvailable(id, checkIn, checkOut, jwtToken));
    }

    @GetMapping("/{id}/available-dates")
    public ResponseEntity<AvailableDatesResponse> getAvailableDates(@RequestHeader("Authorization") String authorizationHeader, @PathVariable Long id) {
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvailableDatesResponse(propertyService.getAvailableDates(id, jwtToken)));
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> propertyExists(@PathVariable Long id) {
        boolean exists = propertyService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    private Property convertPropertyDTOToProperty(PropertyDTO propertyDTO) {
        return modelMapper.map(propertyDTO, Property.class);
    }

    private PropertyDTO convertPropertyToPropertyDTO(Property property) {
        return modelMapper.map(property, PropertyDTO.class);
    }

    @ExceptionHandler
    private ResponseEntity<Object> handleException(PropertyException e) {
        PropertyErrorResponse response = new PropertyErrorResponse(
                e.getMessage(),
                System.currentTimeMillis()
        );

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
