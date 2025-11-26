package com.example.propertyservice.controllers;

import com.example.propertyservice.dto.AvailableDatesResponse;
import com.example.propertyservice.dto.GetPropertyDTO;
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

@RestController
@RequestMapping("${application.endpoint.root}")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<GetPropertyDTO> createProperty(@RequestHeader("Authorization") String authorizationHeader,
                                                         @RequestBody @Valid PropertyDTO propertyDTO, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        Property property = convertPropertyDTOToProperty(propertyDTO);
        String jwtToken = authorizationHeader.replace("Bearer ", "");

        GetPropertyDTO savedProperty = propertyService.save(property, jwtToken);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(savedProperty);
    }

    @PatchMapping(path = "${application.endpoint.id}")
    public ResponseEntity<GetPropertyDTO> updateProperty(@RequestHeader("Authorization") String authorizationHeader,
                                                         @PathVariable("id") Long id, @RequestBody @Valid PropertyDTO propertyDTO, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        Property property = convertPropertyDTOToProperty(propertyDTO);
        String jwtToken = authorizationHeader.replace("Bearer ", "");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(propertyService.updatePropertyById(id, property, jwtToken));
    }

    @DeleteMapping(path = "${application.endpoint.id}")
    public ResponseEntity<HttpStatus> deleteProperty(@PathVariable("id") Long id,
                                                     @RequestHeader("Authorization") String authorizationHeader) {
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        propertyService.delete(id, jwtToken);

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<PropertiesResponse> getAllProperties() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PropertiesResponse(propertyService.findAll()));
    }

    @GetMapping(path = "${application.endpoint.id}")
    public ResponseEntity<GetPropertyDTO> getPropertyById(@PathVariable("id") Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(propertyService.getPropertyById(id));
    }

    @GetMapping(path = "${application.endpoint.search}")
    public ResponseEntity<PropertiesResponse> searchProperties(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        List<GetPropertyDTO> properties = propertyService.search(location, minPrice, maxPrice);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PropertiesResponse(properties));
    }


    @GetMapping(path = "${application.endpoint.availability}")
    public ResponseEntity<Boolean> checkAvailability(@PathVariable("id") Long id,
                                                     @RequestParam LocalDate checkIn,
                                                     @RequestParam LocalDate checkOut) {
        return ResponseEntity.ok(propertyService.isPropertyAvailable(id, checkIn, checkOut));
    }

    @GetMapping(path = "${application.endpoint.available-dates}")
    public ResponseEntity<AvailableDatesResponse> getAvailableDates(@PathVariable("id") Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvailableDatesResponse(propertyService.getAvailableDates(id)));
    }

    @GetMapping(path = "${application.endpoint.exists}")
    public ResponseEntity<Boolean> propertyExists(@PathVariable("id") Long id) {
        Boolean exists = propertyService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    private Property convertPropertyDTOToProperty(PropertyDTO propertyDTO) {
        return modelMapper.map(propertyDTO, Property.class);
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