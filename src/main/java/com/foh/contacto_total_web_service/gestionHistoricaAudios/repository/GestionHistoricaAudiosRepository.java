package com.foh.contacto_total_web_service.gestionHistoricaAudios.repository;

import com.foh.contacto_total_web_service.gestionHistoricaAudios.dto.GestionHistoricaAudiosResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class GestionHistoricaAudiosRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByTramo(String tramo) {
        String jpql = "SELECT idx, DOCUMENTO, CLIENTE, FECHAGESTION, HORAGESTION, TELEFONO, RESULTADO, SOLUCION, USUARIOREGISTRA, ANIO, MES, DIA, NOMBRE " +
                "FROM GESTION_HISTORICA_AUDIOS";

        boolean filtrarPorTramo = tramo != null && !tramo.equalsIgnoreCase("TODOS");

        if (filtrarPorTramo) {
            jpql += " WHERE CARTERA = ?1";
        }

        Query query = entityManager.createNativeQuery(jpql, GestionHistoricaAudiosResponse.class);

        if (filtrarPorTramo) {
            query.setParameter(1, tramo);
        }

        return query.getResultList();
    }


    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDateRange(String startDate, String endDate) {
        //String jpql = "SELECT * FROM GESTION_HISTORICA WHERE FechaGestion BETWEEN ?1 AND ?2";
        String jpql = "SELECT idx, DOCUMENTO, CLIENTE, FECHAGESTION, HORAGESTION, TELEFONO, RESULTADO, SOLUCION, USUARIOREGISTRA, ANIO, MES, DIA, NOMBRE " +
                "FROM GESTION_HISTORICA_AUDIOS " +
                "WHERE FECHAGESTION >= ?1 " +
                "AND FECHAGESTION <= ?2 ";
        //"AND Resultado NOT IN ('FUERA DE SERVICIO - NO EXISTE', 'APAGADO', 'NO CONTESTA', 'MSJ VOZ - SMS - WSP - BAJO PUERTA');";

        Query query = entityManager.createNativeQuery(jpql, GestionHistoricaAudiosResponse.class)
                .setParameter(1, startDate)
                .setParameter(2, endDate);

        return query.getResultList();
    }

    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDocumento(String documento) {
        String jpql = "SELECT idx, DOCUMENTO, CLIENTE, FECHAGESTION, HORAGESTION, TELEFONO, RESULTADO, SOLUCION, USUARIOREGISTRA, ANIO, MES, DIA, NOMBRE " +
                "FROM GESTION_HISTORICA_AUDIOS " +
                "WHERE DOCUMENTO = ?1 ";

        Query query = entityManager.createNativeQuery(jpql, GestionHistoricaAudiosResponse.class)
                .setParameter(1, documento);

        return query.getResultList();
    }

    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByTelefono(String telefono) {
        String jpql = "SELECT idx, DOCUMENTO, CLIENTE, FECHAGESTION, HORAGESTION, TELEFONO, RESULTADO, SOLUCION, USUARIOREGISTRA, ANIO, MES, DIA, NOMBRE " +
                "FROM GESTION_HISTORICA_AUDIOS " +
                "WHERE TELEFONO = ?1 ";

        Query query = entityManager.createNativeQuery(jpql, GestionHistoricaAudiosResponse.class)
                .setParameter(1, telefono);

        return query.getResultList();
    }
}
