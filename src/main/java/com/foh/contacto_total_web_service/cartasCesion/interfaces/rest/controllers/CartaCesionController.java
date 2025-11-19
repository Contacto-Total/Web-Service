package com.foh.contacto_total_web_service.cartasCesion.interfaces.rest.controllers;

import com.foh.contacto_total_web_service.cartasCesion.domain.model.queries.GetCartaCesionByDniQuery;
import com.foh.contacto_total_web_service.cartasCesion.domain.services.CartaCesionQueryService;
import com.foh.contacto_total_web_service.cartasCesion.infrastructure.filesystem.CartaCesionFileService;
import com.foh.contacto_total_web_service.cartasCesion.interfaces.rest.resources.CartaCesionResource;
import com.foh.contacto_total_web_service.shared.interfaces.rest.resources.ErrorResponseResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(value = "/api/cartas-cesion", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cartas de Cesión", description = "Cartas de Cesión Endpoints")
public class CartaCesionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CartaCesionController.class);

    @Autowired
    private CartaCesionQueryService cartaCesionQueryService;

    @Autowired
    private CartaCesionFileService fileService;

    @Operation(
            summary = "Buscar carta de cesión por DNI",
            description = "Busca una carta de cesión en el sistema de archivos utilizando el DNI del cliente (últimos 8 dígitos del nombre del archivo)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carta encontrada",
                    content = @Content(schema = @Schema(implementation = CartaCesionResource.class))),
            @ApiResponse(responseCode = "404", description = "Carta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
            @ApiResponse(responseCode = "400", description = "DNI inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchByDni(@RequestParam String dni) {
        try {
            // Validar que el DNI tenga exactamente 8 dígitos
            if (dni == null || !dni.matches("\\d{8}")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseResource("DNI debe tener exactamente 8 dígitos", "DNI_INVALIDO"));
            }

            var query = new GetCartaCesionByDniQuery(dni);
            Optional<CartaCesionResource> result = cartaCesionQueryService.handle(query);

            if (result.isPresent()) {
                return ResponseEntity.ok(result.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponseResource("No se encontró carta de cesión para el DNI: " + dni, "CARTA_NO_ENCONTRADA"));
            }

        } catch (Exception e) {
            LOGGER.error("Error buscando carta por DNI: {}", dni, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseResource("Error al buscar la carta de cesión", "ERROR_BUSQUEDA"));
        }
    }

    @Operation(
            summary = "Ver carta de cesión",
            description = "Obtiene el contenido del PDF de la carta de cesión para visualización en el navegador."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF encontrado",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Archivo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @GetMapping(value = "/view/{filename}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> viewPdf(@PathVariable String filename) {
        try {
            // Validar que el filename sea seguro (solo alfanuméricos, guiones, guiones bajos y .pdf)
            if (!filename.matches("[a-zA-Z0-9_\\-]+\\.pdf")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseResource("Nombre de archivo inválido", "FILENAME_INVALIDO"));
            }

            Optional<byte[]> pdfContent = fileService.readPdfFile(filename);

            if (pdfContent.isPresent()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .header("X-Frame-Options", "SAMEORIGIN")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdfContent.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ErrorResponseResource("Archivo no encontrado: " + filename, "ARCHIVO_NO_ENCONTRADO"));
            }

        } catch (Exception e) {
            LOGGER.error("Error al ver PDF: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponseResource("Error al cargar el PDF", "ERROR_CARGAR_PDF"));
        }
    }

    @Operation(
            summary = "Descargar carta de cesión",
            description = "Descarga el PDF de la carta de cesión."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF descargado",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Archivo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @GetMapping(value = "/download/{filename}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> downloadPdf(@PathVariable String filename) {
        try {
            // Validar que el filename sea seguro
            if (!filename.matches("[a-zA-Z0-9_\\-]+\\.pdf")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseResource("Nombre de archivo inválido", "FILENAME_INVALIDO"));
            }

            Optional<byte[]> pdfContent = fileService.readPdfFile(filename);

            if (pdfContent.isPresent()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdfContent.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ErrorResponseResource("Archivo no encontrado: " + filename, "ARCHIVO_NO_ENCONTRADO"));
            }

        } catch (Exception e) {
            LOGGER.error("Error al descargar PDF: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponseResource("Error al descargar el PDF", "ERROR_DESCARGAR_PDF"));
        }
    }
}
