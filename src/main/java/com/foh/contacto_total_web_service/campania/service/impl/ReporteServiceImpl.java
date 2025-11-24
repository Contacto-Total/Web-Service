package com.foh.contacto_total_web_service.campania.service.impl;

import com.foh.contacto_total_web_service.compromiso.repository.CompromisoRepository;
import com.foh.contacto_total_web_service.campania.dto.GetFiltersToGenerateFileRequest;
import com.foh.contacto_total_web_service.campania.repository.ReporteRepository;
import com.foh.contacto_total_web_service.campania.service.ReporteService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReporteServiceImpl implements ReporteService {

    @Autowired
    private ReporteRepository reporteRepository;

    @Autowired
    private CompromisoRepository compromisoRepository;

    @Override
    public File getReporteByRangesAndGenerateFile(GetFiltersToGenerateFileRequest getFiltersToGenerateFileRequest) {
        // Actualizar tabla temporal de tipificaciones antes de ejecutar queries
        reporteRepository.actualizarTipificacionMax();

        List<String> promesasCaidas = compromisoRepository.findPromesasCaidasWithoutColchon();
        List<Object[]> resultados = reporteRepository.getReporteByRangos(getFiltersToGenerateFileRequest, promesasCaidas);

        Integer rowCount = 0;

        List<Object[]> listaContactoDirecto = new ArrayList<>();
        List<Object[]> listaContactoIndirecto = new ArrayList<>();
        List<Object[]> listaPromesaRota = new ArrayList<>();
        List<Object[]> listaNoContactado = new ArrayList<>();

        String templatePath = "src/files/modelo_reporte.xlsx";
        String outputPath = "reporte_" + System.currentTimeMillis() + ".xlsx";

        try (FileInputStream fileInputStream = new FileInputStream(templatePath);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            workbook.setSheetName(workbook.getSheetIndex(sheet), "REPORTE");

            CellStyle percentageStyle = workbook.createCellStyle();
            percentageStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));
            percentageStyle.setBorderTop(BorderStyle.THIN);
            percentageStyle.setBorderBottom(BorderStyle.THIN);
            percentageStyle.setBorderLeft(BorderStyle.THIN);
            percentageStyle.setBorderRight(BorderStyle.THIN);

            Row titleRow = sheet.createRow(rowCount);
            titleRow.createCell(0).setCellValue("REPORTE DE CAMPAÑA");
            sheet.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, 3));

            rowCount++;

            Row headerRow = sheet.createRow(rowCount);
            headerRow.createCell(0).setCellValue("TIPO");
            sheet.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, 1));
            headerRow.createCell(2).setCellValue("CANTIDAD");
            headerRow.createCell(3).setCellValue("PORCENTAJE");

            rowCount++;

            for (Object[] resultado : resultados) {
                if (resultado[0].toString().contains("RANGO CONTACTO DIRECTO")) {
                    listaContactoDirecto.add(resultado);
                } else if (resultado[0].toString().contains("RANGO CONTACTO INDIRECTO")) {
                    listaContactoIndirecto.add(resultado);
                } else if (resultado[0].toString().contains("RANGO PROMESA ROTA")) {
                    listaPromesaRota.add(resultado);
                } else if (resultado[0].toString().contains("RANGO NO CONTACTADO")) {
                    listaNoContactado.add(resultado);
                }
            }

            if (!listaContactoDirecto.isEmpty()) {
                if (listaContactoDirecto.size() == 1 && listaContactoDirecto.get(0)[0].toString().contains("RANGO CONTACTO DIRECTO 0 - +")) {
                    Row row = sheet.createRow(rowCount);
                    row.createCell(0).setCellValue("CD");
                    row.createCell(1).setCellValue("CONTACTO DIRECTO");
                    row.createCell(2).setCellValue(Integer.parseInt(listaContactoDirecto.get(0)[1].toString()));
                    row.createCell(3).setCellValue("100%");
                    rowCount++;
                } else {
                    for (Object[] objects : listaContactoDirecto) {
                        Row row = sheet.createRow(rowCount);
                        row.createCell(0).setCellValue("CD");
                        row.createCell(1).setCellValue(objects[0].toString());
                        row.createCell(2).setCellValue(Integer.parseInt(objects[1].toString()));
                        row.createCell(3).setCellValue("100%");
                        rowCount++;
                    }
                }
            }

            if (!listaContactoIndirecto.isEmpty()) {
                if (listaContactoIndirecto.size() == 1 && listaContactoIndirecto.get(0)[0].toString().contains("RANGO CONTACTO INDIRECTO 0 - +")) {
                    Row row = sheet.createRow(rowCount);
                    row.createCell(0).setCellValue("CI");
                    row.createCell(1).setCellValue("CONTACTO INDIRECTO");
                    row.createCell(2).setCellValue(Integer.parseInt(listaContactoIndirecto.get(0)[1].toString()));
                    row.createCell(3).setCellValue("100%");
                    rowCount++;
                } else {
                    for (Object[] objects : listaContactoIndirecto) {
                        Row row = sheet.createRow(rowCount);
                        row.createCell(0).setCellValue("CI");
                        row.createCell(1).setCellValue(objects[0].toString());
                        row.createCell(2).setCellValue(Integer.parseInt(objects[1].toString()));
                        row.createCell(3).setCellValue("100%");
                        rowCount++;
                    }
                }
            }

            if (!listaPromesaRota.isEmpty()) {
                if (listaPromesaRota.size() == 1 && listaPromesaRota.get(0)[0].toString().contains("RANGO PROMESA ROTA 0 - +")) {
                    Row row = sheet.createRow(rowCount);
                    row.createCell(0).setCellValue("PR");
                    row.createCell(1).setCellValue("PROMESA ROTA");
                    row.createCell(2).setCellValue(Integer.parseInt(listaPromesaRota.get(0)[1].toString()));
                    row.createCell(3).setCellValue("100%");
                    rowCount++;
                } else {
                    for (Object[] objects : listaPromesaRota) {
                        Row row = sheet.createRow(rowCount);
                        row.createCell(0).setCellValue("PR");
                        row.createCell(1).setCellValue(objects[0].toString());
                        row.createCell(2).setCellValue(Integer.parseInt(objects[1].toString()));
                        row.createCell(3).setCellValue("100%");
                        rowCount++;
                    }
                }
            }

            if (!listaNoContactado.isEmpty()) {
                if (listaNoContactado.size() == 1 && listaNoContactado.get(0)[0].toString().contains("RANGO NO CONTACTADO 0 - +")) {
                    Row row = sheet.createRow(rowCount);
                    row.createCell(0).setCellValue("NC");
                    row.createCell(1).setCellValue("NO CONTACTADO");
                    row.createCell(2).setCellValue(Integer.parseInt(listaNoContactado.get(0)[1].toString()));
                    row.createCell(3).setCellValue("100%");
                    rowCount++;
                } else {
                    for (Object[] objects : listaNoContactado) {
                        Row row = sheet.createRow(rowCount);
                        row.createCell(0).setCellValue("NC");
                        row.createCell(1).setCellValue(objects[0].toString());
                        row.createCell(2).setCellValue(Integer.parseInt(objects[1].toString()));
                        row.createCell(3).setCellValue("100%");
                        rowCount++;
                    }
                }
            }

            Row totalRow = sheet.createRow(rowCount);
            totalRow.createCell(0).setCellValue("TOTAL");

            int totalCantidad = 0;

            for (Object[] resultado : resultados) {
                totalCantidad += ((Number) resultado[1]).intValue();
            }

            totalRow.createCell(2).setCellValue(totalCantidad);

            for (int i = 2; i < rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row != null && row.getCell(2) != null) {
                    double cantidad = row.getCell(2).getNumericCellValue();  // O Double.parseDouble() si los valores tienen decimales
                    double porcentaje = cantidad / totalCantidad;
                    row.createCell(3).setCellValue(porcentaje);
                    row.getCell(3).setCellStyle(percentageStyle);
                }
            }

            totalRow.createCell(3).setCellValue(1);
            totalRow.getCell(3).setCellStyle(percentageStyle);

            CellStyle borderedStyle = workbook.createCellStyle();
            borderedStyle.setBorderTop(BorderStyle.THIN);
            borderedStyle.setBorderBottom(BorderStyle.THIN);
            borderedStyle.setBorderLeft(BorderStyle.THIN);
            borderedStyle.setBorderRight(BorderStyle.THIN);

            for (int rowIndex = 0; rowIndex <= rowCount; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    for (int colIndex = 0; colIndex <= 3; colIndex++) {
                        Cell cell = row.getCell(colIndex);
                        if (cell == null) {
                            cell = row.createCell(colIndex);
                        }
                        cell.setCellStyle(borderedStyle);
                    }
                }
            }

            for (int i = 2; i <= rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell percentCell = row.getCell(3);
                    if (percentCell != null) {
                        percentCell.setCellStyle(percentageStyle);
                    }
                }
            }

            try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                workbook.write(outputStream);
            }

            return new File(outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getFechasDeVencimiento() {
        return reporteRepository.getFechasDeVencimientoDisponibles();
    }
}
