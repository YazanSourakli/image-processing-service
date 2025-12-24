package com.sourakli.image_processing_service.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sourakli.image_processing_service.model.Image;
import com.sourakli.image_processing_service.repository.ImageRepository;

import lombok.RequiredArgsConstructor;


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
    public Image applyFilter(Long id, String filterType) throws IOException {
        // 1. Das Original-Bild aus der DB holen
        Image originalImage = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bild nicht gefunden mit ID: " + id));

        // 2. Datei von der Festplatte laden
        File inputFile = new File(originalImage.getUrl()); 
        BufferedImage bufferedImage = ImageIO.read(inputFile);

        // 3. Filter-Logik: Graustufen
        if ("grayscale".equalsIgnoreCase(filterType)) {
            BufferedImage grayscaleImage = new BufferedImage(
                    bufferedImage.getWidth(), 
                    bufferedImage.getHeight(), 
                    BufferedImage.TYPE_BYTE_GRAY);
            
            // Das Originalbild in den Graustufen-Container "malen"
            grayscaleImage.getGraphics().drawImage(bufferedImage, 0, 0, null);
            bufferedImage = grayscaleImage; // Ergebnis übernehmen
        } else if ("sepia".equalsIgnoreCase(filterType)){
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int p = bufferedImage.getRGB(x,y);

                    int a = (p>>24)&0xff;
                    int r = (p>>16)&0xff;
                    int g = (p>>8)&0xff;
                    int b = p&0xff;

                    // Neue RGB-Werte berechnen
                    int tr = (int)(0.393*r + 0.769*g + 0.189*b);
                    int tg = (int)(0.349*r + 0.686*g + 0.168*b);
                    int tb = (int)(0.272*r + 0.534*g + 0.131*b);

                    // Werte begrenzen
                    if(tr > 255){ r = 255; } else { r = tr; }
                    if(tg > 255){ g = 255; } else { g = tg; }
                    if(tb > 255){ b = 255; } else { b = tb; }

                    // Setze neuen Pixelwert
                    p = (a<<24) | (r<<16) | (g<<8) | b;
                    bufferedImage.setRGB(x, y, p);
                }
            }
        }
        // 4. Gefiltertes Bild speichern
        String newFileName = System.currentTimeMillis() + "_" + filterType + originalImage.getFileName();
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
    }
}
