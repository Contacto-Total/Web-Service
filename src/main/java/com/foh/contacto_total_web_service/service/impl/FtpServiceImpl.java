package com.foh.contacto_total_web_service.service.impl;

import com.foh.contacto_total_web_service.dto.CreateAudioEvaluacionFileRequest;
import com.foh.contacto_total_web_service.dto.DownloadHistoricoAudioRequest;
import com.foh.contacto_total_web_service.dto.RecordingDateRequest;
import com.foh.contacto_total_web_service.model.FtpConsult;
import com.foh.contacto_total_web_service.repository.CustomFtpConsultRepository;
import com.foh.contacto_total_web_service.repository.FtpConsultRepository;
import com.foh.contacto_total_web_service.service.FtpService;
import com.foh.contacto_total_web_service.util.FileIdentifier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FtpServiceImpl implements FtpService {

    private final DefaultFtpSessionFactory ftpSessionFactory;

    @Autowired
    FtpConsultRepository ftpConsultRepository;

    @Autowired
    CustomFtpConsultRepository customFtpConsultRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public FtpServiceImpl(DefaultFtpSessionFactory ftpSessionFactory) {
        this.ftpSessionFactory = ftpSessionFactory;
    }

    @Override
    public File downloadFile() {
        FtpSession session = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        File localFile = null;

        String remoteFilePath = "2024/09/18/out-901581608-901581608-20240918-155730-1726693050.1010821.WAV";
        String localFileName = "out-901581608-901581608-20240918-155730-1726693050.1010821.WAV";

        try {
            session = ftpSessionFactory.getSession();
            session.read(remoteFilePath, outputStream);

            localFile = new File(localFileName);
            FileCopyUtils.copy(outputStream.toByteArray(), new FileOutputStream(localFile));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return localFile;
    }

    @Override
    public List<String> getRecordingNamesFromDateRange(RecordingDateRequest recordingDateRequest) {
        FtpSession session = null;
        List<String> recordingNames = new ArrayList<>();
        List<FtpConsult> ftpConsults = new ArrayList<>();

        try {
            session = ftpSessionFactory.getSession();
            LocalDate startDate = recordingDateRequest.getStartDate();
            LocalDate endDate = recordingDateRequest.getEndDate();

            ftpConsultRepository.truncateTable();

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String remoteDirectory = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                FTPFile[] files = session.list(remoteDirectory);

                for (FTPFile file : files) {
                    if (file.getName().endsWith(".WAV")) {
                        recordingNames.add(file.getName());

                        String[] parts = file.getName().split("-");
                        if (parts.length >= 5) {
                            FtpConsult ftpConsult = new FtpConsult();
                            ftpConsult.setTelefono(parts[1]);
                            ftpConsult.setFecha(parts[3]);
                            ftpConsult.setHora(parts[4]);
                            ftpConsult.setPathCompleto(file.getName());
                            ftpConsults.add(ftpConsult);
                        }
                    }
                }
            }

            if (!ftpConsults.isEmpty()) {
                customFtpConsultRepository.insertFtpConsults(ftpConsults);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return recordingNames;
    }

    @Override
    public File downloadFileByName(String name) {
        FtpSession session = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        File localFile = null;

        String[] parts = name.split("-");
        if (parts.length > 5) {
            String year = parts[3].substring(0, 4);
            String month = parts[3].substring(4, 6);
            String day = parts[3].substring(6, 8);
            String remoteFilePath = year + "/" + month + "/" + day + "/" + name;

            try {
                session = ftpSessionFactory.getSession();
                session.read(remoteFilePath, outputStream);

                localFile = new File(name);
                FileCopyUtils.copy(outputStream.toByteArray(), new FileOutputStream(localFile));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }

        return localFile;
    }

    @Override
    public File downloadGestionHistoricaAudioFileByName(DownloadHistoricoAudioRequest downloadHistoricoAudioRequest) {
        FtpSession session = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        File localFile = null;

        String remoteFilePath = downloadHistoricoAudioRequest.getAnio() + "/" + downloadHistoricoAudioRequest.getMes() + "/" + downloadHistoricoAudioRequest.getDia() + "/" + downloadHistoricoAudioRequest.getNombre();
        String newName = "audio" +
                "-" +
                downloadHistoricoAudioRequest.getFecha() +
                "-" +
                downloadHistoricoAudioRequest.getResultado() +
                "-" +
                downloadHistoricoAudioRequest.getTelefono() +
                "-" +
                downloadHistoricoAudioRequest.getDocumento() +
                "-" +
                downloadHistoricoAudioRequest.getCliente() +
                "-" +
                downloadHistoricoAudioRequest.getAsesor() +
                ".WAV";

        try {
            session = ftpSessionFactory.getSession();
            session.read(remoteFilePath, outputStream);

            localFile = new File(newName);
            FileCopyUtils.copy(outputStream.toByteArray(), new FileOutputStream(localFile));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return localFile;
    }

    @Override
    public File downloadGestionHistoricaAudiosZip(List<DownloadHistoricoAudioRequest> downloadHistoricoAudioRequests) {
        // Crear archivo ZIP
        File zipFile = new File("audios.zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // Usar un conjunto para evitar duplicados en base al nombre del archivo
            Set<String> addedFiles = new HashSet<>();

            // Iterar sobre la lista de solicitudes y generar un archivo para cada una
            for (DownloadHistoricoAudioRequest request : downloadHistoricoAudioRequests) {
                File generatedFile = downloadGestionHistoricaAudioFileByName(request);

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