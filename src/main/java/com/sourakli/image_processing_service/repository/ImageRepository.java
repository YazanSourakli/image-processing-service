package com.sourakli.image_processing_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sourakli.image_processing_service.model.Image;



@Repository
public interface ImageRepository extends JpaRepository <Image, Long> {
    
}
