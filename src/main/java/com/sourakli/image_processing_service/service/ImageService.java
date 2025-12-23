package com.sourakli.image_processing_service.service;

import com.sourakli.image_processing_service.model.Image;
import com.sourakli.image_processing_service.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor // Erstellt Konstruktor für alle final Felder (Dependency Injection)
public class ImageService {

    private final ImageRepository imageRepository;
    // Hier speichern wir die Bilder Lokal
    private final String uploadDir ="uploads/";

    public Image uploadImage(MultipartFile file) throws IOException {
        // 1. Ordner erstellen, falls nicht existent
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. Datei auf Festplatte speichern
        // Trick: Wir nutzen System.currentTimeMillis() damit Dateinamen eindeutig bleiben
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

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
}
