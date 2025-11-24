package com.foh.contacto_total_web_service.campania.service.impl;

import com.foh.contacto_total_web_service.campania.service.RangoService;
import com.foh.contacto_total_web_service.compromiso.repository.CompromisoRepository;
import com.foh.contacto_total_web_service.campania.dto.GetFiltersToGenerateFileRequest;
import com.foh.contacto_total_web_service.campania.repository.RangoRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class RangoServiceImpl implements RangoService {

    @Autowired
    private RangoRepository rangoRepository;

    @Autowired
    private CompromisoRepository compromisoRepository;

    @Override
    public File getRangosByRangesAndGenerateFile(GetFiltersToGenerateFileRequest getFiltersToGenerateFileRequest) {
        System.out.println("========== [RANGO SERVICE] INICIO ==========");
        long startTime = System.currentTimeMillis();

        System.out.println("[RANGO SERVICE] Obteniendo promesas caídas...");
        List<String> promesasCaidas = compromisoRepository.findPromesasCaidasWithoutColchon();
        System.out.println("[RANGO SERVICE] Promesas caídas obtenidas: " + promesasCaidas.size() + " registros - Tiempo: " + (System.currentTimeMillis() - startTime) + "ms");

        System.out.println("[RANGO SERVICE] Ejecutando query de rangos...");
        long queryStart = System.currentTimeMillis();
        List<Object[]> resultados = rangoRepository.findByRangosAndTipoContacto(getFiltersToGenerateFileRequest, promesasCaidas);
        System.out.println("[RANGO SERVICE] Query completada: " + resultados.size() + " filas - Tiempo: " + (System.currentTimeMillis() - queryStart) + "ms");

        String templatePath = "src/files/modelo_campana_asterisk.xlsm";
        String outputPath = "rangos_resultados.xlsm";

        try {
            System.out.println("[RANGO SERVICE] Abriendo archivo Excel template...");
            FileInputStream fileInputStream = new FileInputStream(templatePath);
            Workbook workbook = new XSSFWorkbook(fileInputStream);

            System.out.println("[RANGO SERVICE] Escribiendo " + resultados.size() + " filas en Excel...");
            long excelStart = System.currentTimeMillis();
            Sheet sheet = workbook.getSheetAt(0);
            int rowNum = 1;

            for (Object[] row : resultados) {
                Row newRow = sheet.createRow(rowNum++);
                newRow.createCell(0).setCellValue(row[0] != null ? row[0].toString() : "");
                newRow.createCell(1).setCellValue(row[1] != null ? row[1].toString() : "");
            }
            System.out.println("[RANGO SERVICE] Filas escritas - Tiempo: " + (System.currentTimeMillis() - excelStart) + "ms");

            System.out.println("[RANGO SERVICE] Guardando archivo Excel...");
            long saveStart = System.currentTimeMillis();
            try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                workbook.write(outputStream);
            }
            workbook.close();
            fileInputStream.close();
            System.out.println("[RANGO SERVICE] Archivo guardado - Tiempo: " + (System.currentTimeMillis() - saveStart) + "ms");

            System.out.println("========== [RANGO SERVICE] FIN - Tiempo total: " + (System.currentTimeMillis() - startTime) + "ms ==========");
            return new File(outputPath);
        } catch (IOException e) {
            System.err.println("[RANGO SERVICE] ERROR al generar archivo: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
