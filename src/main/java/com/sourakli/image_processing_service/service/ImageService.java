package com.sourakli.image_processing_service.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sourakli.image_processing_service.model.Image;
import com.sourakli.image_processing_service.repository.ImageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service-Klasse zur Verwaltung von Bildern.
 * <p>
 * Diese Klasse kümmert sich um den Upload, die Speicherung im Dateisystem,
 * die Datenbank-Einträge und die Anwendung von Filtern.
 */
@Service
@RequiredArgsConstructor // Erstellt Konstruktor für alle final Felder (Dependency Injection)
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;
    private final FilterService filterService; // Dependency Injection
    // Hier speichern wir die Bilder Lokal
    @Value("${image.upload.dir}") 
    private String uploadDir;

    // Whitelist für erlaubte MIME-Types (Sicherheit)
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", 
            "image/png", 
            "image/jpg"
    );

    /**
     * Lädt ein Bild hoch und speichert es im Dateisystem sowie in der Datenbank.
     * Führt Validierungen für leere Dateien und unerlaubte Dateitypen durch.
     *
     * @param file Die hochgeladene Datei (MultipartFile)
     * @return Das gespeicherte Image-Objekt (Entity)
     * @throws IOException Wenn Fehler beim Schreiben auf die Festplatte auftreten
     * @throws IllegalArgumentException Wenn die Datei leer ist oder einen falschen Typ hat
     */
    public Image uploadImage(MultipartFile file) throws IOException {
        // 1. Validierung: Datei darf nicht leer sein
        if (file.isEmpty()) {
            log.warn("Nutzer hat versucht, eine leere Datei hochzuladen.");
            throw new IllegalArgumentException("Die hochgeladene Datei darf nicht leer sein.");
        }
        // 2. Validierung des Dateityps (MIME-Type)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Ungültiger Dateityp versucht: {}", contentType);
            throw new IllegalArgumentException("Ungültiger Dateityp! Erlaubt sind nur: " + ALLOWED_CONTENT_TYPES);
        }
        // 3. Ordner erstellen, falls nicht existent
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Upload-Verzeichnis erstellt: {}", uploadDir);
        }

        // 4. Datei speichern
        String fileName = generateFileName(file.getOriginalFilename(), null);
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        log.info("Datei erfolgreich gespeichert: {}", fileName);
        // 5. Datenbank-Eintrag erstellen (Entity bauen)        
        Image image = Image.builder()
                .fileName(fileName)
                .contentTyp(contentType)
                .size(file.getSize())
                .url(filePath.toString().replace("\\", "/"))
                .uploadTime(LocalDateTime.now())
                .build();
        // 6. In DB speichern und zurückgeben
        return imageRepository.save(image);
    }

    /**
     * Wendet einen Bildfilter auf ein bereits gespeichertes Bild an.
     * Erstellt eine Kopie des Bildes mit dem angewendeten Effekt.
     *
     * @param id Die ID des Originalbildes in der Datenbank
     * @param filterType Der Name des Filters (z.B. "sepia", "grayscale")
     * @return Das neu erstellte, gefilterte Image-Objekt
     * @throws IOException Bei Fehlern beim Lesen/Schreiben der Bilddatei
     * @throws RuntimeException Wenn das Bild mit der angegebenen ID nicht existiert
     * @throws IllegalArgumentException Wenn der angegebene Filter-Typ unbekannt ist
     */
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

    /**
     * Hilfsmethode zum Generieren eines eindeutigen Dateinamens mit Zeitstempel.
     * Format: YYYY-MM-DD_HH-mm-ss_[Suffix]_OriginalName
     * @param originalFileName Der ursprüngliche Name der Datei
     * @param suffix Ein optionaler Zusatz (z.B. Filtername), kann null sein
     * @return Der generierte, eindeutige Dateiname
     */
    private String generateFileName(String originalFileName, String suffix) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    // Falls ein Suffix (z.B. "sepia") da ist, fügen wir es ein, sonst nur den Timestamp
    String prefix = (suffix != null && !suffix.isEmpty()) ? timestamp + "_" + suffix + "_" : timestamp + "_";
    return prefix + originalFileName;
}
}
