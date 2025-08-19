package com.foh.contacto_total_web_service.gestionHistorica.repository;

import com.foh.contacto_total_web_service.gestionHistorica.dto.GestionHistoricaResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GestionHistoricaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<GestionHistoricaResponse> getGestionHistoricaByDateRange(String startDate, String endDate) {
        //String jpql = "SELECT * FROM GESTION_HISTORICA WHERE FechaGestion BETWEEN ?1 AND ?2";
        String jpql = "SELECT Documento, Cliente, Telefono, FechaGestion, HoraUbicacion, Resultado, TipoGestion, Solucion " +
                "FROM GESTION_HISTORICA " +
                "WHERE HoraUbicacion != '00:00:00' " +
                "AND (FechaGestion >= ?1 AND FechaGestion <= ?2) ";
                //"AND Resultado NOT IN ('FUERA DE SERVICIO - NO EXISTE', 'APAGADO', 'NO CONTESTA', 'MSJ VOZ - SMS - WSP - BAJO PUERTA');";

        Query query = entityManager.createNativeQuery(jpql, GestionHistoricaResponse.class)
                .setParameter(1, startDate)
                .setParameter(2, endDate);

        return query.getResultList();
    }
}
