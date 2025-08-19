package com.foh.contacto_total_web_service.plantillaSMS.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foh.contacto_total_web_service.sms.dto.GenerateMessagesRequest;
import com.foh.contacto_total_web_service.sms.dto.PeopleForSMSResponse;
import com.foh.contacto_total_web_service.plantillaSMS.dto.PlantillaSMSRequest;
import com.foh.contacto_total_web_service.plantillaSMS.dto.PlantillaSMSToUpdateRequest;
import com.foh.contacto_total_web_service.plantillaSMS.model.PlantillaSMS;
import com.foh.contacto_total_web_service.compromiso.repository.CompromisoRepository;
import com.foh.contacto_total_web_service.plantillaSMS.repository.PlantillaSMSRepository;
import com.foh.contacto_total_web_service.sms.repository.SMSRepository;
import com.foh.contacto_total_web_service.plantillaSMS.service.PlantillaSMSService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@Service
public class PlantillaSMSServiceImpl implements PlantillaSMSService {

    @Autowired
    private PlantillaSMSRepository plantillaSMSRepository;

    @Autowired
    private SMSRepository smsRepository;

    @Autowired
    private CompromisoRepository compromisoRepository;

    @Override
    public PlantillaSMS createPlantillaSMS(PlantillaSMSRequest plantillaSMSRequest) {
        PlantillaSMS plantillaSMS = new PlantillaSMS();
        plantillaSMS.setName(plantillaSMSRequest.getName());
        plantillaSMS.setTemplate(plantillaSMSRequest.getTemplate());

        ObjectMapper mapper = new ObjectMapper();
        try {
            String tipisJson = mapper.writeValueAsString(plantillaSMSRequest.getTipis());
            plantillaSMS.setTipis(tipisJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al convertir los campos a JSON", e);
        }

        return plantillaSMSRepository.save(plantillaSMS);
    }

    @Override
    public List<PlantillaSMS> getPlantillasSMS() {
        return plantillaSMSRepository.findAll();
    }

    @Override
    public PlantillaSMS getPlantillaById(Integer id) {
        return plantillaSMSRepository.findById(id).orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));
    }

    @Override
    public PlantillaSMS getPlantillaByNombre(String name) {
        return plantillaSMSRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con el nombre: " + name));
    }

    @Override
    public File getFileByPlantillaWithData(GenerateMessagesRequest generateMessagesRequest) {
        PlantillaSMS plantilla = getPlantillaByNombre(generateMessagesRequest.getName());
        List<String> tipis = generateMessagesRequest.getTipis();
        List<String> promesas;

        if (tipis.contains("PROMESA DE PAGO")) {
            promesas = compromisoRepository.findAllPromesasExceptVigentes();
        } else {
            promesas = compromisoRepository.findAllPromesasExceptCaidasToSMS();
        }

        List<PeopleForSMSResponse> peopleData = smsRepository.getPeopleForSMS(tipis, promesas);

        LocalDate today = LocalDate.now();
        int dia = today.getDayOfMonth();

        if (today.getMonth() != Month.FEBRUARY && dia == today.lengthOfMonth()) {
        } else if (dia < today.lengthOfMonth()) {
            dia += 1;
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Mensajes SMS");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("CELULAR");
        headerRow.createCell(1).setCellValue("var1");
        headerRow.createCell(2).setCellValue("var2");

        for (int i = 3; i <= 15; i++) {
            headerRow.createCell(i).setCellValue("var" + (i));
        }

        int rowNum = 1;
        for (PeopleForSMSResponse person : peopleData) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(person.getTelefonoCelular());

            String nombreCompleto = person.getNombre();
            String primerNombre = nombreCompleto.split(" ")[0];

            String mensaje = plantilla.getTemplate()
                    .replace("{documento}", person.getDocumento())
                    .replace("{nombre}", primerNombre)
                    .replace("{telefonoCelular}", person.getTelefonoCelular())
                    .replace("{deudaTotal}", String.valueOf(person.getDeudaTotal()))
                    .replace("{ltd}", String.valueOf(person.getLtd()))
                    .replace("{dia}", String.valueOf(dia));

            if (mensaje.length() > 160) {
                for (int i = primerNombre.length(); i > 0; i--) {
                    String nombreCortado = primerNombre.substring(0, i);
                    String mensajeAjustado = mensaje.replace(primerNombre, nombreCortado);
                    if (mensajeAjustado.length() <= 160) {
                        mensaje = mensajeAjustado;
                        break;
                    }
                }
            }

            row.createCell(1).setCellValue(mensaje);

            row.createCell(2).setCellValue(mensaje.length());

            for (int i = 3; i <= 15; i++) {
                row.createCell(i).setCellValue("");
            }
        }

        File file = new File("Mensajes_SMS.xlsx");
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            throw new RuntimeException("Error al crear el archivo Excel", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    @Override
    public File getFileByPlantillaWithData2(GenerateMessagesRequest generateMessagesRequest) {
        PlantillaSMS plantilla = getPlantillaByNombre(generateMessagesRequest.getName());

        List<PeopleForSMSResponse> peopleData = smsRepository.getPeopleForSMS2(generateMessagesRequest.getName());

        LocalDate today = LocalDate.now();
        int dia = today.getDayOfMonth();

        if (today.getMonth() != Month.FEBRUARY && dia == today.lengthOfMonth()) {
        } else if (dia < today.lengthOfMonth()) {
            dia += 1;
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Mensajes SMS");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("CELULAR");
        headerRow.createCell(1).setCellValue("var1");
        headerRow.createCell(2).setCellValue("var2");

        for (int i = 3; i <= 15; i++) {
            headerRow.createCell(i).setCellValue("var" + (i));
        }

        int rowNum = 1;
        for (PeopleForSMSResponse person : peopleData) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(person.getTelefonoCelular());

            String nombreCompleto = person.getNombre();
            String primerNombre = nombreCompleto.split(" ")[0];

            String mensaje = plantilla.getTemplate()
                    .replace("{documento}", person.getDocumento())
                    .replace("{nombre}", primerNombre)
                    .replace("{telefonoCelular}", person.getTelefonoCelular())
                    .replace("{deudaTotal}", String.valueOf(person.getDeudaTotal()))
                    .replace("{ltd}", String.valueOf(person.getLtd()))
                    .replace("{dia}", String.valueOf(dia));

            if (mensaje.length() > 160) {
                for (int i = primerNombre.length(); i > 0; i--) {
                    String nombreCortado = primerNombre.substring(0, i);
                    String mensajeAjustado = mensaje.replace(primerNombre, nombreCortado);
                    if (mensajeAjustado.length() <= 160) {
                        mensaje = mensajeAjustado;
                        break;
                    }
                }
            }

            row.createCell(1).setCellValue(mensaje);

            row.createCell(2).setCellValue(mensaje.length());

            for (int i = 3; i <= 15; i++) {
                row.createCell(i).setCellValue("");
            }
        }

        File file = new File("Mensajes_SMS.xlsx");
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            throw new RuntimeException("Error al crear el archivo Excel", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    @Override
    public PlantillaSMS updatePlantilla(PlantillaSMSToUpdateRequest plantillaSMSToUpdateRequest) {
        PlantillaSMS plantilla = getPlantillaById(plantillaSMSToUpdateRequest.getId());
        plantilla.setName(plantillaSMSToUpdateRequest.getName());
        plantilla.setTemplate(plantillaSMSToUpdateRequest.getTemplate());

        ObjectMapper mapper = new ObjectMapper();
        try {
            String tipisJson = mapper.writeValueAsString(plantillaSMSToUpdateRequest.getTipis());
            plantilla.setTipis(tipisJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al convertir los campos a JSON", e);
        }

        return plantillaSMSRepository.save(plantilla);
    }

    @Override
    public void deletePlantilla(Integer id) {
        PlantillaSMS plantilla = getPlantillaById(id);
        plantillaSMSRepository.delete(plantilla);
    }
}
