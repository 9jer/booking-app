package com.example.propertyservice.controllers;

import com.example.propertyservice.dto.ImageDTO;
import com.example.propertyservice.services.ImageService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping(value = "/{propertyId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ImageDTO>> uploadImages(
            @PathVariable Long propertyId,
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok(imageService.uploadImages(propertyId, files, token));
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws MalformedURLException {
        Path file = Paths.get("uploads").resolve(filename);
        Resource resource = new UrlResource(file.toUri());

        String contentType;
        try {
            contentType = Files.probeContentType(file);
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        if (resource.exists() || resource.isReadable()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
