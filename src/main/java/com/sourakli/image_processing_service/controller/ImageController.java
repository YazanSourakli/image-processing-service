package com.sourakli.image_processing_service.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sourakli.image_processing_service.model.Image;
import com.sourakli.image_processing_service.service.ImageService;

import lombok.RequiredArgsConstructor;


/**
 * REST-Controller für die Bildverarbeitung.
 * Stellt Endpunkte zum Hochladen und Filtern von Bildern bereit.
 */
@RestController
@RequestMapping("api/images") // Basis-URL für alle Endpunkte in diesem Controller
@RequiredArgsConstructor

public class ImageController {
    private final ImageService imageService;

    /**
     * Lädt ein neues Bild hoch.
     *
     * @param file Die Bilddatei als Multipart-Form-Data
     * @return Das gespeicherte Bild mit Metadaten (HTTP 200) oder Fehler bei ungültigen Daten (HTTP 400/500)
     */
    @PostMapping(value="/upload", consumes = "multipart/form-data")
    public ResponseEntity<Image> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            Image savedImage = imageService.uploadImage(file);
            return ResponseEntity.ok(savedImage);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Wendet einen Filter auf ein existierendes Bild an.
     *
     * @param id Die ID des Bildes
     * @param filterType Der Name des Filters (z.B. "sepia", "grayscale")
     * @return Das bearbeitete Bild (HTTP 200) oder 404 falls nicht gefunden
     */
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
    
    /**
     * Fängt Validierungsfehler (z.B. falscher Dateityp) ab und sendet eine saubere Fehlermeldung.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidArguments(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body("Fehler: " + e.getMessage());
    }
}