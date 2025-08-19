package com.foh.contacto_total_web_service.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
public class CompromisoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<String> findAllPromesas() {
        String jpql = "SELECT DOCUMENTO FROM COMPROMISOS";

        Query query = entityManager.createNativeQuery(jpql);

        return query.getResultList();
    }

    public List<String> findPromesasCaidas() {
        LocalDate today = LocalDate.now();

        if (today.getDayOfMonth() == 2) {
            return List.of();
        }

        LocalDate twoDaysAgo = today.minusDays(2);
        String formattedDate = twoDaysAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String jpql = "SELECT DOCUMENTO FROM COMPROMISOS WHERE IMPORTE_PAGO_MENSUAL = 0 " +
                "AND ESTADO_COMPROMISO = 'CAIDO' AND FECHA_COMPROMISO <= ?1";

        Query query = entityManager.createNativeQuery(jpql);
        query.setParameter(1, formattedDate);

        return query.getResultList();
    }

    public List<String> findAllPromesasExceptVigentes() {
        String jpql = "SELECT DOCUMENTO FROM COMPROMISOS WHERE ESTADO_COMPROMISO != 'VIGENTE'";

        Query query = entityManager.createNativeQuery(jpql);

        return query.getResultList();
    }

    public List<String> findAllPromesasExceptCaidasToSMS() {
        List<String> promesasCaidas = findPromesasCaidas();

        if (promesasCaidas.isEmpty()) {
            return findAllPromesas();
        }

        String jpql = "SELECT DOCUMENTO FROM COMPROMISOS WHERE DOCUMENTO NOT IN (?1)";
        Query query = entityManager.createNativeQuery(jpql);
        query.setParameter(1, promesasCaidas);

        return query.getResultList();
    }

    public List<String> findPromesasCaidasWithoutColchon() {
        LocalDate today = LocalDate.now();

        if (today.getDayOfMonth() == 2) {
            return List.of();
        }

        LocalDate twoDaysAgo = today.minusDays(2);
        String formattedDate = twoDaysAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String jpql = "SELECT DOCUMENTO FROM COMPROMISOS WHERE IMPORTE_PAGO_MENSUAL = 0 " +
                "AND ESTADO_COMPROMISO = 'CAIDO' AND FECHA_COMPROMISO <= ?1 " +
                "AND DOCUMENTO NOT IN ( " +
                "SELECT DISTINCT Documento FROM GESTION_HISTORICA " +
                "WHERE Resultado IN ('PROMESA DE PAGO', 'OPORTUNIDAD DE PAGO') " +
                "AND (Observacion LIKE '%(CONVENIO)%' " +
                "OR Observacion LIKE '%(EXCEPCION)%') " +
                "AND FechaGestion <= DATE_FORMAT(CURDATE(), '%Y-%m-03') "+
                ")";

        Query query = entityManager.createNativeQuery(jpql);
        query.setParameter(1, formattedDate);

        return query.getResultList();
    }
}
