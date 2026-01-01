package com.sourakli.image_processing_service.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock; // Für die Checks (Asserts)
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify; // Für die Mocks (when, verify)
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile; // Hilft beim rekursiven Löschen
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;

import com.sourakli.image_processing_service.model.Image;
import com.sourakli.image_processing_service.repository.ImageRepository;


@ExtendWith(MockitoExtension.class) // Sagt JUnit: "Nutze Mockito, um Mocks zu initialisieren"
public class ImageSericeTest {
    @Mock // Erstellt eine Attrappe des Repositories (ruft KEINE echte DB auf)
    private ImageRepository imageRepository;

    @Mock // Erstellt eine Attrappe des FilterService
    private FilterService filterService;

    @InjectMocks // Erstellt den echten ImageService und spritzt die Mocks (oben) dort ein
    private ImageService imageService;

    @BeforeEach
    void setUp() {
        // Problem: @Value("${image.upload.dir}") funktioniert in Unit Tests ohne Spring Context nicht.
        // Lösung: Wir setzen den Wert manuell per Reflection.
        ReflectionTestUtils.setField(imageService, "uploadDir", "test-uploads/");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Löscht den Ordner "test-uploads/" und alles, was darin ist
        Path path = Paths.get("test-uploads/");
        FileSystemUtils.deleteRecursively(path);
    }

    /**
     * Testfall 1: Happy Path (Alles läuft gut)
     * Szenario: Ein gültiges JPEG wird hochgeladen.
     * Erwartung: Es wird gespeichert und das Repository wird aufgerufen.
     */
    @Test
    void testUploadImage_Success() throws IOException {
        // 1. VORBEREITUNG (Arrange)
        // Wir simulieren eine Datei (Name, Originalname, Content-Type, Inhalt)
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.jpg", 
                "image/jpeg", 
                "dummy content".getBytes()
        );

        // Wir sagen dem Mock-Repository: "Wenn 'save' aufgerufen wird, gib einfach ein Image-Objekt zurück"
        Image expectedImage = Image.builder().fileName("test.jpg").build();
        when(imageRepository.save(any(Image.class))).thenReturn(expectedImage);

        // 2. DURCHFÜHRUNG (Act)
        // Wir rufen die echte Methode auf
        Image result = imageService.uploadImage(file);

        // 3. PRÜFUNG (Assert)
        assertNotNull(result); // Es darf nicht null zurückkommen
        assertEquals("test.jpg", result.getFileName()); // Der Name muss stimmen
        
        // Profi-Check: Wurde imageRepository.save() wirklich genau 1x aufgerufen?
        verify(imageRepository, times(1)).save(any(Image.class));
    }
}
