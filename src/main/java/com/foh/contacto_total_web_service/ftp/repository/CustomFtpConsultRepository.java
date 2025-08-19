package com.foh.contacto_total_web_service.ftp.repository;

import com.foh.contacto_total_web_service.ftp.model.FtpConsult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class CustomFtpConsultRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void insertFtpConsults(List<FtpConsult> ftpConsults) {
        if (ftpConsults.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ftp_consult (telefono, fecha, hora, path_completo) VALUES ");
        for (int i = 0; i < ftpConsults.size(); i++) {
            FtpConsult ftpConsult = ftpConsults.get(i);
            sql.append("('")
                    .append(ftpConsult.getTelefono()).append("', '")
                    .append(ftpConsult.getFecha()).append("', '")
                    .append(ftpConsult.getHora()).append("', '")
                    .append(ftpConsult.getPathCompleto()).append("')");

            if (i < ftpConsults.size() - 1) {
                sql.append(", ");
            }
        }

        entityManager.createNativeQuery(sql.toString()).executeUpdate();
    }
}
