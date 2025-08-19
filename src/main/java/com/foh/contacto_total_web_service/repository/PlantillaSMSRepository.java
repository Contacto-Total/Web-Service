package com.foh.contacto_total_web_service.repository;

import com.foh.contacto_total_web_service.model.PlantillaSMS;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlantillaSMSRepository extends JpaRepository<PlantillaSMS, Integer> {
    Optional<PlantillaSMS> findByName(String name);
}
