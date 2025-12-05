package com.sourakli.image_processing_service.model;

import  java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity // Sagt Spring: "Mach daraus eine Datenbank-Tabelle"
@Data   // Lombok: Generiert Getter, Setter, toString, etc. automatisch
@NoArgsConstructor // Lombok: Leerer Konstruktor (braucht JPA)
@AllArgsConstructor // Lombok: Konstruktor mit allen Argumenten
@Builder // Lombok: Cooles Design-Pattern zum Erstellen von Objekten

public class Image {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String contentTyp;
    private Long size;
    private String url;
    private LocalDateTime uploadTime;
    

    
}
