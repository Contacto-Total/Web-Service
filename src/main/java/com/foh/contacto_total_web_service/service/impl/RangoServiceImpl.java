package com.foh.contacto_total_web_service.service.impl;

import com.foh.contacto_total_web_service.dto.GetRangosByRangesAndGenerateFileRequest;
import com.foh.contacto_total_web_service.repository.CompromisoRepository;
import com.foh.contacto_total_web_service.repository.RangoRepository;
import com.foh.contacto_total_web_service.service.RangoService;
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
    public File getRangosByRangesAndGenerateFile(GetRangosByRangesAndGenerateFileRequest getRangosByRangesAndGenerateFileRequest) {
        List<String> promesasCaidas = compromisoRepository.findPromesasCaidasWithoutColchon();
        List<Object[]> resultados = rangoRepository.findByRangosAndTipoContacto(getRangosByRangesAndGenerateFileRequest, promesasCaidas);

        String templatePath = "src/files/modelo_campana_asterisk.xlsm";
        String outputPath = "rangos_resultados.xlsm";

        try (FileInputStream fileInputStream = new FileInputStream(templatePath);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowNum = 1;

            for (Object[] row : resultados) {
                Row newRow = sheet.createRow(rowNum++);
                newRow.createCell(0).setCellValue(row[0] != null ? row[0].toString() : "");
                newRow.createCell(1).setCellValue(row[1] != null ? row[1].toString() : "");
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
}
