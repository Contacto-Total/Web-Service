package com.foh.contacto_total_web_service.repository;

import com.foh.contacto_total_web_service.dto.BlacklistRequest;
import com.foh.contacto_total_web_service.dto.BlacklistResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
public class BlacklistRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<BlacklistResponse> getAll() {
        String sql = "SELECT EMPRESA, CARTERA, SUBCARTERA, DOCUMENTO, EMAIL, TELEFONO, FECHA_INICIO, FECHA_FIN FROM blacklist";
        Query query = entityManager.createNativeQuery(sql, BlacklistResponse.class);
        return query.getResultList();
    }

    public void insert(BlacklistRequest blacklist, String email, String telefono) {
        try {
            LocalDate currentDate = LocalDate.now();
            String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            String sql = "INSERT INTO blacklist (EMPRESA, CARTERA, SUBCARTERA, DOCUMENTO, EMAIL, TELEFONO, FECHA_INICIO, FECHA_FIN) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, blacklist.getEmpresa());
            query.setParameter(2, blacklist.getCartera());
            query.setParameter(3, blacklist.getSubcartera());
            query.setParameter(4, blacklist.getDocumento());
            query.setParameter(5, email);
            query.setParameter(6, telefono);
            query.setParameter(7, formattedDate);
            query.setParameter(8, blacklist.getFechaFin());
            query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error al insertar en la tabla blacklist: " + e.getMessage(), e);
        }
    }
}