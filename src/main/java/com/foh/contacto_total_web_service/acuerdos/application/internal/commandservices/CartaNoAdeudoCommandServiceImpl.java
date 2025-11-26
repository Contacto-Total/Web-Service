package com.foh.contacto_total_web_service.acuerdos.application.internal.commandservices;

import com.foh.contacto_total_web_service.acuerdos.domain.model.command.CreateCartaNoAdeudoCommand;
import com.foh.contacto_total_web_service.acuerdos.domain.services.CartaNoAdeudoCommandService;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

@Service
public class CartaNoAdeudoCommandServiceImpl implements CartaNoAdeudoCommandService {

    private final TemplateEngine templateEngine;

    public CartaNoAdeudoCommandServiceImpl(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public byte[] handle(CreateCartaNoAdeudoCommand command) throws IOException {
        System.out.println("=== INICIO handle() Carta No Adeudo ===");
        System.out.println("Entidad recibida: " + command.entidad());

        // Determinar el template según la entidad (por ahora solo nsoluciones)
        String templateName = switch (command.entidad().toLowerCase()) {
            case "nsoluciones" -> "acuerdos/nsoluciones/template_no_adeudo";
            // Futuro: case "financiera_oh" -> "acuerdos/financiera_oh/template_no_adeudo";
            default -> throw new IllegalArgumentException("Entidad no soportada para carta de no adeudo: " + command.entidad());
        };

        System.out.println("Template seleccionado: " + templateName);

        // Crear contexto de Thymeleaf
        Context context = new Context();
        context.setVariable("nombreCompleto", command.nombreCompleto());
        context.setVariable("dni", command.dni());
        context.setVariable("numeroCuenta", command.numeroCuenta());
        context.setVariable("fechaActual", command.fechaActual());
        context.setVariable("fechaCancelacion", command.fechaCancelacion());
        context.setVariable("rucFinanciera", command.rucFinanciera());
        context.setVariable("rucNsoluciones", command.rucNsoluciones());

        // Ruta base absoluta para las imágenes
        String basePath = Paths.get("src/main/resources/static/").toAbsolutePath().toUri().toString();
        context.setVariable("basePath", basePath);

        // Renderizar HTML con Thymeleaf
        String htmlContent = templateEngine.process(templateName, context);

        // Convertir HTML a PDF con Flying Saucer
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent, basePath);
        renderer.layout();
        renderer.createPDF(baos);
        baos.close();

        return baos.toByteArray();
    }
}
