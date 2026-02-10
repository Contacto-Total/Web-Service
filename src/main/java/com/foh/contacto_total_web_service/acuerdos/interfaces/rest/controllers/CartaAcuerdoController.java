package com.foh.contacto_total_web_service.acuerdos.interfaces.rest.controllers;

import com.foh.contacto_total_web_service.acuerdos.domain.model.command.CreateCartaAcuerdoCommand;
import com.foh.contacto_total_web_service.acuerdos.domain.model.command.CreateCartaNoAdeudoCommand;
import com.foh.contacto_total_web_service.acuerdos.domain.model.queries.GetDatosByClienteQuery;
import com.foh.contacto_total_web_service.acuerdos.domain.services.CartaAcuerdoCommandService;
import com.foh.contacto_total_web_service.acuerdos.domain.services.CartaAcuerdoQueryService;
import com.foh.contacto_total_web_service.acuerdos.domain.services.CartaNoAdeudoCommandService;
import com.foh.contacto_total_web_service.acuerdos.infrastructure.persistence.jpa.repositories.CartaAcuerdoRepository;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources.CreateCartaAcuerdoResource;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources.CreateCartaNoAdeudoResource;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources.DatosAcuerdoResource;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.transform.CreateCartaAcuerdoCommandFromResourceAssembler;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.transform.CreateCartaNoAdeudoCommandFromResourceAssembler;
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

    @Autowired
    private CartaAcuerdoRepository cartaAcuerdoRepository;

    @Autowired
    private CartaNoAdeudoCommandService cartaNoAdeudoCommandService;

    @Operation(
            summary = "Obtener datos del cliente",
            description = "Devuelve los datos de acuerdo de un cliente según su DNI, si existe un acuerdo válido."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos encontrados del cliente",
                    content = @Content(schema = @Schema(implementation = DatosAcuerdoResource.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado en la base de datos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
            @ApiResponse(responseCode = "422", description = "Cliente sin promesa de pago u oportunidad de pago",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @GetMapping("/datos-cliente/{dni}")
    public ResponseEntity<?> getDatosCliente(@PathVariable String dni) {
        try {
            // Primero verificar si el cliente existe en la base de datos
            boolean clienteExiste = cartaAcuerdoRepository.clienteExisteEnTempMerge(dni);

            if (!clienteExiste) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponseResource("Cliente no encontrado en la base de datos", "CLIENTE_NO_ENCONTRADO"));
            }

            // Si existe, buscar si tiene promesa de pago u oportunidad de pago
            var query = new GetDatosByClienteQuery(dni);
            var datosClienteResource = cartaAcuerdoQueryService.handle(query);

            if (datosClienteResource.isPresent()) {
                return ResponseEntity.ok(datosClienteResource.get());
            } else {
                // Fallback: devolver datos básicos de TEMP_MERGE sin promesa
                var basicData = cartaAcuerdoRepository.findBasicDataByDni(dni);
                if (basicData.isPresent()) {
                    return ResponseEntity.ok(basicData.get());
                }
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(new ErrorResponseResource("El cliente no tiene datos registrados", "SIN_DATOS"));
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
            LOGGER.info("Generando carta de acuerdo para entidad: {}, DNI: {}", resource.entidad(), resource.dni());

            var command = CreateCartaAcuerdoCommandFromResourceAssembler.toCommandFromResource(resource);
            byte[] pdfBytes = cartaAcuerdoCommandService.handle(command);

            LOGGER.info("Carta generada exitosamente para entidad: {}", resource.entidad());

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

    @Operation(
            summary = "Generar carta de no adeudo",
            description = "Genera un archivo PDF con los datos de la carta de no adeudo del cliente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carta generada correctamente",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @PostMapping(value = "/carta-no-adeudo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> generarCartaNoAdeudo(@Valid @RequestBody CreateCartaNoAdeudoResource resource) {
        try {
            LOGGER.info("Generando carta de no adeudo para entidad: {}, DNI: {}", resource.entidad(), resource.dni());

            var command = CreateCartaNoAdeudoCommandFromResourceAssembler.toCommandFromResource(resource);
            byte[] pdfBytes = cartaNoAdeudoCommandService.handle(command);

            LOGGER.info("Carta de no adeudo generada exitosamente para entidad: {}", resource.entidad());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"carta_no_adeudo.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseResource(e.getMessage(), "DATOS_INVALIDOS"));
        } catch (Exception e) {
            LOGGER.error("Error al generar carta de no adeudo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseResource("Error al generar la carta", "ERROR_GENERAR_CARTA"));
        }
    }

}
