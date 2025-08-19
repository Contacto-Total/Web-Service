package com.foh.contacto_total_web_service.service.impl;

import com.foh.contacto_total_web_service.dto.AudioEvaluacionResponse;
import com.foh.contacto_total_web_service.dto.CreateAudioEvaluacionFileRequest;
import com.foh.contacto_total_web_service.repository.AudioEvaluacionRepository;
import com.foh.contacto_total_web_service.service.AudioEvaluacionService;
import com.foh.contacto_total_web_service.util.FileIdentifier;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class AudioEvaluacionServiceImpl implements AudioEvaluacionService {

    @Autowired
    AudioEvaluacionRepository AudioEvalucionRepository;

    @Override
    public File createAudioEvaluationFile(CreateAudioEvaluacionFileRequest createAudioEvaluationFileRequest) {
        AudioEvaluacionResponse audioEvaluationResponse = AudioEvalucionRepository.getAudioEvaluationById(createAudioEvaluationFileRequest.getGestionHistoricaAudioIdx());

        if (audioEvaluationResponse == null) {
            System.out.println("No se encontró la evaluación de audio con el id: " + createAudioEvaluationFileRequest.getGestionHistoricaAudioIdx());
            return null;
        }

        String templatePath = "";
        String outputPath = "";

        switch (createAudioEvaluationFileRequest.getResultado()) {
            case "CONTACTO CON TITULAR O ENCARGADO":
                templatePath = "src/files/evaluacion_de_calidad_CD.xlsx";
                break;
            case "PROMESA DE PAGO":
                templatePath = "src/files/evaluacion_de_calidad_PDP.xlsx";
                break;
        }

        String newFechaGestion = createAudioEvaluationFileRequest.getFecha().replace("-", "");

        outputPath = "reporte-" +
                newFechaGestion +
                "-" +
                createAudioEvaluationFileRequest.getResultado() +
                "-" +
                createAudioEvaluationFileRequest.getTelefono() +
                "-" +
                createAudioEvaluationFileRequest.getDni() +
                "-" +
                createAudioEvaluationFileRequest.getCliente() +
                "-" +
                createAudioEvaluationFileRequest.getAsesor() +
                ".xlsx";

        try (FileInputStream fileInputStream = new FileInputStream(templatePath);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            sheet.getRow(3).getCell(2).setCellValue(createAudioEvaluationFileRequest.getAsesor());

            sheet.getRow(6).getCell(2).setCellValue(createAudioEvaluationFileRequest.getFecha());

            sheet.getRow(7).getCell(2).setCellValue(audioEvaluationResponse.getPresAsert());

            sheet.getRow(8).getCell(2).setCellValue(audioEvaluationResponse.getPresNomApe());

            sheet.getRow(9).getCell(2).setCellValue(audioEvaluationResponse.getOrigLlamada());

            sheet.getRow(10).getCell(2).setCellValue(audioEvaluationResponse.getMotivLlamada());

            sheet.getRow(11).getCell(2).setCellValue(audioEvaluationResponse.getMotivNoPago());

            sheet.getRow(12).getCell(2).setCellValue(audioEvaluationResponse.getManejoObjec());

            sheet.getRow(13).getCell(2).setCellValue(audioEvaluationResponse.getOpcionesPago());

            sheet.getRow(14).getCell(2).setCellValue(audioEvaluationResponse.getBeneficioPago());

            sheet.getRow(15).getCell(2).setCellValue(audioEvaluationResponse.getFechaHoraPago());

            sheet.getRow(16).getCell(2).setCellValue(audioEvaluationResponse.getDatosAdicionales());

            sheet.getRow(17).getCell(2).setCellValue(audioEvaluationResponse.getConfirmMonto());

            sheet.getRow(18).getCell(2).setCellValue(audioEvaluationResponse.getConfirmFecha());

            sheet.getRow(19).getCell(2).setCellValue(audioEvaluationResponse.getConfirmCanal());

            sheet.getRow(20).getCell(2).setCellValue(audioEvaluationResponse.getConsecPago());

            sheet.getRow(21).getCell(2).setCellValue(
                            audioEvaluationResponse.getPresAsert() +
                            audioEvaluationResponse.getPresNomApe() +
                            audioEvaluationResponse.getOrigLlamada() +
                            audioEvaluationResponse.getMotivLlamada() +
                            audioEvaluationResponse.getMotivNoPago() +
                            audioEvaluationResponse.getManejoObjec() +
                            audioEvaluationResponse.getOpcionesPago() +
                            audioEvaluationResponse.getBeneficioPago() +
                            audioEvaluationResponse.getFechaHoraPago() +
                            audioEvaluationResponse.getDatosAdicionales() +
                            audioEvaluationResponse.getConfirmMonto() +
                            audioEvaluationResponse.getConfirmFecha() +
                            audioEvaluationResponse.getConfirmCanal() +
                            audioEvaluationResponse.getConsecPago()
            );

            sheet.getRow(24).getCell(2).setCellValue(createAudioEvaluationFileRequest.getDni());

            sheet.getRow(25).getCell(2).setCellValue(createAudioEvaluationFileRequest.getTelefono());

            sheet.getRow(26).getCell(2).setCellValue(createAudioEvaluationFileRequest.getFecha());

            sheet.getRow(28).getCell(2).setCellValue(
                            audioEvaluationResponse.getPresAsert() +
                            audioEvaluationResponse.getPresNomApe() +
                            audioEvaluationResponse.getOrigLlamada() +
                            audioEvaluationResponse.getMotivLlamada()
            );

            sheet.getRow(29).getCell(2).setCellValue(
                            audioEvaluationResponse.getMotivNoPago() +
                            audioEvaluationResponse.getManejoObjec() +
                            audioEvaluationResponse.getOpcionesPago() +
                            audioEvaluationResponse.getBeneficioPago() +
                            audioEvaluationResponse.getFechaHoraPago()
            );

            sheet.getRow(30).getCell(2).setCellValue(
                            audioEvaluationResponse.getDatosAdicionales() +
                            audioEvaluationResponse.getConfirmMonto() +
                            audioEvaluationResponse.getConfirmFecha() +
                            audioEvaluationResponse.getConfirmCanal() +
                            audioEvaluationResponse.getConsecPago()
            );

            sheet.addMergedRegion(new CellRangeAddress(34, 52, 1, 2)); // Unir celdas C33 a F33
            sheet.getRow(34).getCell(1).setCellValue(audioEvaluationResponse.getSummary());
            sheet.getRow(34).getCell(1).setCellStyle(createWrappedTextCellStyle(workbook));

            try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                workbook.write(outputStream);
            }

            return new File(outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static CellStyle createWrappedTextCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    @Override
    public File createAudioEvaluationsZip(List<CreateAudioEvaluacionFileRequest> createAudioEvaluations) {
        // Crear archivo ZIP
        File zipFile = new File("resultado_audio_evaluations.zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // Usar un conjunto para evitar duplicados en base al nombre del archivo
            Set<String> addedFiles = new HashSet<>();

            // Iterar sobre la lista de solicitudes y generar un archivo para cada una
            for (CreateAudioEvaluacionFileRequest request : createAudioEvaluations) {
                File generatedFile = createAudioEvaluationFile(request);

                if (generatedFile != null && generatedFile.exists()) {
                    String fileName = generatedFile.getName();
                    // Si el archivo ya ha sido agregado, agregar un sufijo para hacerlo único
                    if (addedFiles.contains(fileName)) {
                        String newFileName = FileIdentifier.generateUniqueFileName(fileName);
                        fileName = newFileName;
                    }
                    addedFiles.add(fileName);  // Marcar este archivo como agregado

                    // Agregar el archivo generado al ZIP
                    try (FileInputStream fis = new FileInputStream(generatedFile)) {
                        ZipEntry zipEntry = new ZipEntry(fileName);
                        zos.putNextEntry(zipEntry);

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();

                        // Eliminar el archivo después de agregarlo al ZIP
                        generatedFile.delete();
                    }
                }
            }

            return zipFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
