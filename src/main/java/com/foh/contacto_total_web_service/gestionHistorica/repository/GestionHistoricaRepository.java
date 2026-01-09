package com.foh.contacto_total_web_service.gestionHistorica.repository;

import com.foh.contacto_total_web_service.gestionHistorica.dto.GestionHistoricaClienteResponse;
import com.foh.contacto_total_web_service.gestionHistorica.dto.GestionHistoricaResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GestionHistoricaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<GestionHistoricaResponse> getGestionHistoricaByDateRange(String startDate, String endDate) {
        String jpql = "SELECT Documento, Cliente, Telefono, FechaGestion, HoraUbicacion, Resultado, TipoGestion, Solucion " +
                "FROM GESTION_HISTORICA " +
                "WHERE HoraUbicacion != '00:00:00' " +
                "AND (FechaGestion >= ?1 AND FechaGestion <= ?2) ";

        Query query = entityManager.createNativeQuery(jpql, GestionHistoricaResponse.class)
                .setParameter(1, startDate)
                .setParameter(2, endDate);

        return query.getResultList();
    }

    /**
     * Obtiene gestiones históricas de un cliente con paginación
     */
    public List<GestionHistoricaClienteResponse> getGestionesByDocumento(String documento, int page, int size) {
        String sql = """
            SELECT
                g.FechaGestion,
                g.HoraGestion,
                CASE WHEN g.UsuarioRegistra = 'PROGRESIVO' THEN 'SISTEMA' ELSE g.UsuarioRegistra END AS Agente,
                CASE
                    WHEN g.Resultado IN ('CANCELACION TOTAL', 'CANCELACION PARCIAL') THEN 'CANCELACION'
                    ELSE g.Resultado
                END AS Resultado,
                CASE
                    WHEN g.Resultado IN ('CANCELACION TOTAL', 'CANCELACION PARCIAL') THEN g.Resultado
                    ELSE g.Solucion
                END AS Solucion,
                g.Telefono,
                g.Observacion,
                CASE
                    WHEN g.TipoGestion = 'LLAMADA SALIENTE' THEN 'LLAMADA_SALIENTE'
                    WHEN g.TipoGestion = 'LLAMADA ENTRANTE' THEN 'LLAMADA_ENTRANTE'
                    WHEN g.TipoGestion = 'WSP' THEN 'WHATSAPP'
                    ELSE g.TipoGestion
                END AS Canal,
                CASE
                    WHEN g.UsuarioRegistra = 'PROGRESIVO' THEN 'GESTION_PROGRESIVO'
                    ELSE 'GESTION_MANUAL'
                END AS Metodo,
                COALESCE(NULLIF(g.ImporteCompromiso, 0), g.ImporteOportunidad) AS MontoPromesa,
                CASE
                    WHEN p.Estado = 'Pagada' THEN 'PAGADA'
                    WHEN p.Estado = 'Caida' THEN 'VENCIDA'
                    WHEN p.Estado = 'Vigente' THEN 'PENDIENTE'
                    ELSE NULL
                END AS EstadoPromesa
            FROM GESTION_HISTORICA_BI g
            LEFT JOIN PROMESAS_HISTORICO p
                ON g.Documento = p.Documento
                AND g.FechaGestion = p.FechaGestion
                AND (g.FechaCompromiso = p.FechaCompromiso OR g.FechaOportunidad = p.FechaOportunidad)
            WHERE g.UsuarioRegistra <> 'PROGRESIVO'
                AND g.Documento = ?1
            ORDER BY g.FechaGestion DESC, g.HoraGestion DESC
            LIMIT ?2 OFFSET ?3
            """;

        int offset = page * size;

        Query query = entityManager.createNativeQuery(sql)
                .setParameter(1, documento)
                .setParameter(2, size)
                .setParameter(3, offset);

        List<Object[]> results = query.getResultList();
        List<GestionHistoricaClienteResponse> responses = new ArrayList<>();

        for (Object[] row : results) {
            responses.add(GestionHistoricaClienteResponse.builder()
                    .fechaGestion(row[0] != null ? row[0].toString() : null)
                    .horaGestion(row[1] != null ? row[1].toString() : null)
                    .agente(row[2] != null ? row[2].toString() : null)
                    .resultado(row[3] != null ? row[3].toString() : null)
                    .solucion(row[4] != null ? row[4].toString() : null)
                    .telefono(row[5] != null ? row[5].toString() : null)
                    .observacion(row[6] != null ? row[6].toString() : null)
                    .canal(row[7] != null ? row[7].toString() : null)
                    .metodo(row[8] != null ? row[8].toString() : null)
                    .montoPromesa(row[9] != null ? new BigDecimal(row[9].toString()) : null)
                    .estadoPromesa(row[10] != null ? row[10].toString() : null)
                    .build());
        }

        return responses;
    }

    /**
     * Cuenta el total de gestiones históricas de un cliente
     */
    public long countGestionesByDocumento(String documento) {
        String sql = """
            SELECT COUNT(*)
            FROM GESTION_HISTORICA_BI g
            WHERE g.UsuarioRegistra <> 'PROGRESIVO'
                AND g.Documento = ?1
            """;

        Query query = entityManager.createNativeQuery(sql)
                .setParameter(1, documento);

        return ((Number) query.getSingleResult()).longValue();
    }
}
