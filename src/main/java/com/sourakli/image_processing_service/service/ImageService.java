package com.sourakli.image_processing_service.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files; // Import für Logging
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sourakli.image_processing_service.model.Image;
import com.sourakli.image_processing_service.repository.ImageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor // Erstellt Konstruktor für alle final Felder (Dependency Injection)
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;
    private final FilterService filterService; // Dependency Injection
    // Hier speichern wir die Bilder Lokal
    @Value("${image.upload.dir}") 
    private String uploadDir;

    public Image uploadImage(MultipartFile file) throws IOException {
        // 0. Validierung: Datei darf nicht leer sein
        if (file.isEmpty()) {
            log.warn("Nutzer hat versucht, eine leere Datei hochzuladen.");
            throw new IllegalArgumentException("Die hochgeladene Datei darf nicht leer sein.");
        }
        // 1. Ordner erstellen, falls nicht existent
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Upload-Verzeichnis erstellt: {}", uploadDir);
        }

        // 2. Datei auf Festplatte speichern
        // Trick: Wir nutzen System.currentTimeMillis() damit Dateinamen eindeutig bleiben
        String fileName = generateFileName(file.getOriginalFilename(), null);
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        log.info("Datei erfolgreich gespeichert: {}", fileName);
        // 3. Datenbank-Eintrag erstellen (Entity bauen)        
        Image image = Image.builder()
                .fileName(fileName)
                .contentTyp(file.getContentType())
                .size(file.getSize())
                .url(filePath.toString().replace("\\", "/"))
                .uploadTime(LocalDateTime.now())
                .build();
        // 4. In DB speichern und zurückgeben
        return imageRepository.save(image);
    }
    public Image applyFilter(Long id, String filterType) throws IOException {
        // 1. Das Original-Bild aus der DB holen
        Image originalImage = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bild nicht gefunden mit ID: " + id));

        // 2. Datei von der Festplatte laden
        File inputFile = new File(originalImage.getUrl()); 
        BufferedImage bufferedImage = ImageIO.read(inputFile);

        // 3. Filter anwenden (Delegation an den FilterService)
        if ("grayscale".equalsIgnoreCase(filterType)) {
            bufferedImage = filterService.applyGrayscale(bufferedImage);
        } else if ("sepia".equalsIgnoreCase(filterType)) {
            bufferedImage = filterService.applySepia(bufferedImage);
        } else {
             throw new IllegalArgumentException("Unbekannter Filter: " + filterType);
        }
        // 4. Gefiltertes Bild speichern
        String newFileName = generateFileName(originalImage.getFileName(), filterType);
        Path uploadPath = Paths.get(uploadDir);
        Path newFilePath = uploadPath.resolve(newFileName);

        File outputFile = new File(newFilePath.toString());
        ImageIO.write(bufferedImage, "jpg", outputFile);

        // 5. Neuen DB-Eintrag erstellen
        Image filteredImage = Image.builder()
                .fileName(newFileName)
                .contentTyp("image/jpeg")
                .size(Files.size(newFilePath))
                .url(newFilePath.toString().replace("\\", "/"))
                .uploadTime(LocalDateTime.now())
                .build();

        return imageRepository.save(filteredImage);
    } // Ende applyFilter

    // Hilfsmethode für lesbare Dateinamen
    private String generateFileName(String originalFileName, String suffix) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    // Falls ein Suffix (z.B. "sepia") da ist, fügen wir es ein, sonst nur den Timestamp
    String prefix = (suffix != null && !suffix.isEmpty()) ? timestamp + "_" + suffix + "_" : timestamp + "_";
    return prefix + originalFileName;
}
}
