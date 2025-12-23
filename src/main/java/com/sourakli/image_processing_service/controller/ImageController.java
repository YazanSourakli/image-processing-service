package com.sourakli.image_processing_service.controller;

import com.sourakli.image_processing_service.service.ImageService;
import com.sourakli.image_processing_service.model.Image;
import com.sourakli.image_processing_service.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("api/images") // Basis-URL für alle Endpunkte in diesem Controller
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;

    @PostMapping(value="/upload", consumes = "multipart/form-data")
    public ResponseEntity<Image> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            Image savedImage = imageService.uploadImage(file);
            return ResponseEntity.ok(savedImage);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}