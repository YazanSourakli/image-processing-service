package com.sourakli.image_processing_service.service;

import java.awt.image.BufferedImage;

import org.springframework.stereotype.Service;


/**
 * Enthält die mathematischen Algorithmen zur Bildmanipulation.
 * Arbeitet direkt auf den Pixeldaten (BufferedImage).
 */
@Service
public class FilterService {
    /**
     * Konvertiert ein Farbbild in Graustufen.
     * Nutzt den Standard-Typ TYPE_BYTE_GRAY für performante Umwandlung.
     *
     * @param original Das Originalbild
     * @return Ein neues BufferedImage in Graustufen
     */
    public BufferedImage applyGrayscale(BufferedImage original) {
        BufferedImage grayscaleImage = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        grayscaleImage.getGraphics().drawImage(original, 0, 0, null);
        return grayscaleImage;
    }

    /**
     * Wendet einen Sepia-Effekt (altmodischer, gelb-brauner Look) an.
     * Iteriert über jeden Pixel und berechnet neue RGB-Werte basierend auf einer Standard-Sepia-Formel.
     *
     * @param original Das Originalbild (wird direkt modifiziert)
     * @return Das modifizierte Bild im Sepia-Look
     */
    public BufferedImage applySepia(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Wir arbeiten direkt auf dem Bild weiter
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = original.getRGB(x, y);

                int a = (p >> 24) & 0xff;
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                int tr = (int)(0.393*r + 0.769*g + 0.189*b);
                int tg = (int)(0.349*r + 0.686*g + 0.168*b);
                int tb = (int)(0.272*r + 0.534*g + 0.131*b);

                if(tr > 255) tr = 255;
                if(tg > 255) tg = 255;
                if(tb > 255) tb = 255;

                p = (a<<24) | (tr<<16) | (tg<<8) | tb;
                original.setRGB(x, y, p);
            }
        }
        return original;
    }

}
