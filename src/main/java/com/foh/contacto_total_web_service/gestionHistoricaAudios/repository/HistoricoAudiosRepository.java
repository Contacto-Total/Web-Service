package com.foh.contacto_total_web_service.gestionHistoricaAudios.repository;

import com.foh.contacto_total_web_service.gestionHistoricaAudios.dto.HistoricoAudiosRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class HistoricoAudiosRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public void insertAllHistoricoAudios(List<HistoricoAudiosRequest> historicoAudiosRequests) {
        if (historicoAudiosRequests.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder("INSERT INTO HISTORICO_AUDIOS (anio, mes, dia, nombre, duracion, peso) VALUES ");
        for (int i = 0; i < historicoAudiosRequests.size(); i++) {
            HistoricoAudiosRequest historicoAudiosRequest = historicoAudiosRequests.get(i);
            sql.append("('")
                    .append(historicoAudiosRequest.getAnio()).append("', '")
                    .append(historicoAudiosRequest.getMes()).append("', '")
                    .append(historicoAudiosRequest.getDia()).append("', '")
                    .append(historicoAudiosRequest.getNombre()).append("', '")
                    .append(historicoAudiosRequest.getDuracion()).append("', '")
                    .append(historicoAudiosRequest.getPeso()).append("')");

            if (i < historicoAudiosRequests.size() - 1) {
                sql.append(", ");
            }
        }

        entityManager.createNativeQuery(sql.toString()).executeUpdate();
    }
}
