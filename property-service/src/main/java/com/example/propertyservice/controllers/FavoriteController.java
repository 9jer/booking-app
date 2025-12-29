package com.example.propertyservice.controllers;

import com.example.propertyservice.dto.GetPropertyDTO;
import com.example.propertyservice.services.PropertyService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final PropertyService propertyService;

    @PostMapping("/{propertyId}")
    public ResponseEntity<Void> toggleFavorite(
            @PathVariable Long propertyId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {

        String token = authorizationHeader.replace("Bearer ", "");
        propertyService.toggleFavorite(propertyId, token);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<GetPropertyDTO>> getFavorites(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader,
            @PageableDefault(size = 10) Pageable pageable) {

        String token = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok(propertyService.getUserFavorites(token, pageable));
    }
}
