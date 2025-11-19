package com.foh.contacto_total_web_service.cartasCesion.infrastructure.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class CartaCesionFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CartaCesionFileService.class);
    private static final String CARTAS_DIRECTORY = "/home/ubuntu/cashi/Cartas/";

    /**
     * Busca un archivo PDF por DNI (últimos 8 dígitos del nombre del archivo)
     * Formato esperado: XXXXX_carta_DDDDDDDD.pdf donde D = dígito del DNI
     */
    public Optional<String> findPdfByDni(String dni) {
        try {
            File directory = new File(CARTAS_DIRECTORY);

            if (!directory.exists() || !directory.isDirectory()) {
                LOGGER.error("Directorio de cartas no existe: {}", CARTAS_DIRECTORY);
                return Optional.empty();
            }

            File[] files = directory.listFiles();
            if (files == null) {
                return Optional.empty();
            }

            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".pdf")) {
                    String filename = file.getName();

                    // Extraer últimos 8 dígitos antes de .pdf
                    // Formato: 00001_carta_12345678.pdf
                    String nameWithoutExtension = filename.substring(0, filename.lastIndexOf(".pdf"));

                    // Obtener últimos 8 caracteres
                    if (nameWithoutExtension.length() >= 8) {
                        String lastEightDigits = nameWithoutExtension.substring(nameWithoutExtension.length() - 8);

                        if (lastEightDigits.equals(dni)) {
                            LOGGER.info("Carta encontrada para DNI {}: {}", dni, filename);
                            return Optional.of(filename);
                        }
                    }
                }
            }

            LOGGER.warn("No se encontró carta para DNI: {}", dni);
            return Optional.empty();

        } catch (Exception e) {
            LOGGER.error("Error buscando carta para DNI: {}", dni, e);
            return Optional.empty();
        }
    }

    /**
     * Lee el contenido de un archivo PDF como bytes
     */
    public Optional<byte[]> readPdfFile(String filename) {
        try {
            Path filePath = Paths.get(CARTAS_DIRECTORY, filename);

            if (!Files.exists(filePath)) {
                LOGGER.error("Archivo no encontrado: {}", filename);
                return Optional.empty();
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            LOGGER.info("Archivo leído correctamente: {} ({} bytes)", filename, fileContent.length);
            return Optional.of(fileContent);

        } catch (IOException e) {
            LOGGER.error("Error leyendo archivo: {}", filename, e);
            return Optional.empty();
        }
    }

    /**
     * Verifica si un archivo existe
     */
    public boolean fileExists(String filename) {
        Path filePath = Paths.get(CARTAS_DIRECTORY, filename);
        return Files.exists(filePath);
    }
}
