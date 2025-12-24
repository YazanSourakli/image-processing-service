package com.sourakli.image_processing_service.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sourakli.image_processing_service.model.Image;
import com.sourakli.image_processing_service.service.ImageService;

import lombok.RequiredArgsConstructor;

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
    @PostMapping("/{id}/filter")
    public ResponseEntity<Image> applyFilter(
        @PathVariable long id, 
        @RequestParam("type") String filterType) {
            try {
                // Ruft die Logik im Service auf
                Image filteredImage = imageService.applyFilter(id, filterType);
                return ResponseEntity.ok(filteredImage);
            } catch (IOException e) {
                return ResponseEntity.status(500).build(); // Interner Serverfehler bei IO-Problemen
            } catch (RuntimeException e) {
                return ResponseEntity.status(404).build(); // Bild nicht gefunden
            }
        }
}