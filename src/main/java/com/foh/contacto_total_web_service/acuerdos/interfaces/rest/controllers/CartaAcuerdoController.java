package com.foh.contacto_total_web_service.acuerdos.interfaces.rest.controllers;

import com.foh.contacto_total_web_service.acuerdos.domain.model.command.CreateCartaAcuerdoCommand;
import com.foh.contacto_total_web_service.acuerdos.domain.model.queries.GetDatosByClienteQuery;
import com.foh.contacto_total_web_service.acuerdos.domain.services.CartaAcuerdoCommandService;
import com.foh.contacto_total_web_service.acuerdos.domain.services.CartaAcuerdoQueryService;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources.CreateCartaAcuerdoResource;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources.DatosAcuerdoResource;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.transform.CreateCartaAcuerdoCommandFromResourceAssembler;
import com.foh.contacto_total_web_service.shared.interfaces.rest.resources.ErrorResponseResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping(value = "/api/cartas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cartas de Acuerdo", description = "Cartas de acuerdo Endpoints")
public class CartaAcuerdoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CartaAcuerdoController.class);

    @Autowired
    private CartaAcuerdoCommandService cartaAcuerdoCommandService;

    @Autowired
    private CartaAcuerdoQueryService cartaAcuerdoQueryService;

    @Operation(
            summary = "Obtener datos del cliente",
            description = "Devuelve los datos de acuerdo de un cliente según su DNI y tramo, si existe un acuerdo válido."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos encontrados del cliente",
                    content = @Content(schema = @Schema(implementation = DatosAcuerdoResource.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron datos para el cliente y tramo especificados",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @GetMapping("/datos-cliente/{dni}/{tramo}")
    public ResponseEntity<?> getDatosCliente(@PathVariable String dni, @PathVariable String tramo) {
        try {
            var query = new GetDatosByClienteQuery(dni, tramo);
            var datosClienteResource = cartaAcuerdoQueryService.handle(query);

            if (datosClienteResource.isPresent()) {
                return ResponseEntity.ok(datosClienteResource.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponseResource("No se encontraron datos", "DATOS_NO_ENCONTRADOS"));
            }

        } catch (Exception e) {
            LOGGER.error("Error al obtener datos del cliente", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseResource("Error al obtener datos del cliente", "ERROR_DATOS_CLIENTE"));
        }
    }

    @Operation(
            summary = "Generar carta de acuerdo",
            description = "Genera un archivo PDF con los datos de la carta de acuerdo del cliente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carta generada correctamente",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @PostMapping(value = "/carta-acuerdo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> generarCartaAcuerdo(@Valid @RequestBody CreateCartaAcuerdoResource resource) {
        try {

            var command = CreateCartaAcuerdoCommandFromResourceAssembler.toCommandFromResource(resource);
            byte[] pdfBytes = cartaAcuerdoCommandService.handle(command);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"carta_acuerdo.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseResource(e.getMessage(), "DATOS_INVALIDOS"));
        } catch (Exception e) {
            LOGGER.error("Error al generar carta de acuerdo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseResource("Error al generar la carta", "ERROR_GENERAR_CARTA"));
        }
    }

}
