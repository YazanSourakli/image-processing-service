package com.sourakli.image_processing_service.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks; // Für die Checks (Asserts)
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times; // Für die Mocks (when, verify)
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension; // Hilft beim rekursiven Löschen
import org.springframework.mock.web.MockMultipartFile;
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
    // Hilfsmethode: Erstellt ein echtes Mini-Bild auf der Festplatte für Tests
    private void createDummyFile(String filename) throws IOException {
        Path path = Paths.get("test-uploads/");
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        
        // Ein 10x10 schwarzes Bild erstellen
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        File outputFile = new File("test-uploads/" + filename);
        ImageIO.write(img, "jpg", outputFile);
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

    /**
     * Testfall 2: Fehlerfall - Leere Datei
     * Szenario: Eine Datei mit 0 Bytes wird hochgeladen.
     * Erwartung: Der Service wirft eine IllegalArgumentException.
     */
    @Test
    void testUploadImage_EmptyFile_ThrowsException() {
        // 1. ARRANGE
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", 
                "empty.jpg", 
                "image/jpeg", 
                new byte[0] // Leer!
        );

        // 2. ACT & ASSERT
        // Wir prüfen: "Wenn ich das aufrufe, MUSS es knallen"
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageService.uploadImage(emptyFile);
        });

        // Optional: Prüfen, ob die Fehlermeldung stimmt
        assertEquals("Die hochgeladene Datei darf nicht leer sein.", exception.getMessage());
        
        // WICHTIG: Sicherstellen, dass NICHTS gespeichert wurde
        verify(imageRepository, never()).save(any());
    }

    /**
     * Testfall 3: Fehlerfall - Falscher Dateityp
     * Szenario: Eine Word-Datei wird hochgeladen.
     * Erwartung: Exception wegen falschem MIME-Type.
     */
    @Test
    void testUploadImage_WrongType_ThrowsException() {
        // 1. ARRANGE
        MockMultipartFile wrongFile = new MockMultipartFile(
                "file", 
                "test.docx", 
                "application/msword", // Falscher Typ
                "content".getBytes()
        );

        // 2. ACT & ASSERT
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageService.uploadImage(wrongFile);
        });

        assertTrue(exception.getMessage().contains("Ungültiger Dateityp"));
        verify(imageRepository, never()).save(any());
    }
    /**
     * Testfall 4: Filter erfolgreich anwenden
     * Szenario: Bild existiert, User will "grayscale".
     * Erwartung: FilterService wird gerufen, neues Bild wird gespeichert.
     */
    @Test
    void testApplyFilter_Grayscale_Success() throws IOException {
        // 1. ARRANGE
        // Wir brauchen eine echte Datei, sonst wirft ImageIO einen Fehler
        String filename = "original.jpg";
        createDummyFile(filename);

        // Wir simulieren den DB-Eintrag
        Image mockImage = Image.builder()
                .id(1L)
                .fileName(filename)
                .url("test-uploads/" + filename) // Pfad zu unserem Dummy-Bild
                .build();

        // Wenn DB gefragt wird: Gib das Bild zurück
        when(imageRepository.findById(1L)).thenReturn(java.util.Optional.of(mockImage));

        // Wenn FilterService gefragt wird: Gib einfach das Original zurück (reicht für den Test)
        when(filterService.applyGrayscale(any())).thenReturn(new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY));
        
        // Mocking für save
        when(imageRepository.save(any(Image.class))).thenReturn(Image.builder().fileName("new.jpg").build());

        // 2. ACT
        imageService.applyFilter(1L, "grayscale");

        // 3. ASSERT
        // Wurde der richtige Filter aufgerufen?
        verify(filterService, times(1)).applyGrayscale(any());
        // Wurde das Ergebnis gespeichert?
        verify(imageRepository, times(1)).save(any(Image.class));
    }

    /**
     * Testfall 5: Bild nicht gefunden
     * Szenario: User gibt ID 99 an, die gibt es nicht.
     * Erwartung: RuntimeException.
     */
    @Test
    void testApplyFilter_ImageNotFound() {
        // DB sagt: Nichts gefunden (Empty)
        when(imageRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            imageService.applyFilter(99L, "sepia");
        });
    }

    /**
     * Testfall 6: Ungültiger Filter
     * Szenario: User will Filter "disco-lights" (gibt es nicht).
     * Erwartung: IllegalArgumentException.
     */
    @Test
    void testApplyFilter_InvalidFilterType() throws IOException {
        // Datei erstellen & DB Mocken (damit wir bis zur Filter-Prüfung kommen)
        String filename = "original.jpg";
        createDummyFile(filename);
        
        Image mockImage = Image.builder().id(1L).url("test-uploads/" + filename).build();
        when(imageRepository.findById(1L)).thenReturn(java.util.Optional.of(mockImage));

        // Test
        assertThrows(IllegalArgumentException.class, () -> {
            imageService.applyFilter(1L, "disco-lights");
        });
    }
}
