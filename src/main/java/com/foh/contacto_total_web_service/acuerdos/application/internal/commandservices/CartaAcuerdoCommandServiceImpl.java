package com.foh.contacto_total_web_service.acuerdos.application.internal.commandservices;

import com.foh.contacto_total_web_service.acuerdos.domain.model.command.CreateCartaAcuerdoCommand;
import com.foh.contacto_total_web_service.acuerdos.domain.services.CartaAcuerdoCommandService;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

@Service
public class CartaAcuerdoCommandServiceImpl implements CartaAcuerdoCommandService {

    private final TemplateEngine templateEngine;

    public CartaAcuerdoCommandServiceImpl(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public byte[] handle(CreateCartaAcuerdoCommand command) throws IOException {
        // Crear contexto de Thymeleaf
        Context context = new Context();
        context.setVariable("nombreTitular", command.nombreTitular());
        context.setVariable("dni", command.dni());
        context.setVariable("cuentaTarjeta", command.cuentaTarjeta());
        context.setVariable("fechaActual", command.fechaActual());
        context.setVariable("fechaGestion", command.fechaCompromiso());
        context.setVariable("deudaTotal", command.deudaTotal());
        context.setVariable("descuento", command.descuento());
        context.setVariable("montoAprobado", command.montoAprobado());
        context.setVariable("formasPago", command.formasDePago());

        // Ruta base absoluta para las imágenes
        String basePath = Paths.get("src/files/").toAbsolutePath().toUri().toString();
        context.setVariable("basePath", basePath);

        // Renderizar HTML con Thymeleaf
        String htmlContent = templateEngine.process("template_acuerdo", context);

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
