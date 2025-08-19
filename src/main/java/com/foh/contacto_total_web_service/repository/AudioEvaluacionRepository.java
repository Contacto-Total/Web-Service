package com.foh.contacto_total_web_service.repository;

import com.foh.contacto_total_web_service.dto.AudioEvaluacionResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

@Repository
public class AudioEvaluacionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }

        return ((Number) value).intValue();
    }

    public AudioEvaluacionResponse getAudioEvaluationById(Integer id) {
        String jpql = "SELECT ae.id, ae.filename, ae.pres_asert, ae.pres_nom_ape, ae.orig_llamada, " +
                "ae.motiv_llamada, ae.motiv_no_pago, ae.manejo_objec, ae.opciones_pago, ae.beneficio_pago, " +
                "ae.fecha_hora_pago, ae.datos_adicionales, ae.confirm_monto, ae.confirm_fecha, ae.confirm_canal, " +
                "ae.consec_pago, ae.summary, ae.gestion_historica_audios_id FROM AUDIOS_EVALUACIONES ae WHERE ae.gestion_historica_audios_id = ?1";

        Query query = entityManager.createNativeQuery(jpql)
                .setParameter(1, id);

        try {
            // Usar getSingleResult() pero con manejo de excepción
            Object[] result = (Object[]) query.getSingleResult();

            AudioEvaluacionResponse response = new AudioEvaluacionResponse();

            response.setId((Integer) result[0]);
            response.setFilename((String) result[1]);
            response.setPresAsert(toInteger(result[2]));
            response.setPresNomApe(toInteger(result[3]));
            response.setOrigLlamada(toInteger(result[4]));
            response.setMotivLlamada(toInteger(result[5]));
            response.setMotivNoPago(toInteger(result[6]));
            response.setManejoObjec(toInteger(result[7]));
            response.setOpcionesPago(toInteger(result[8]));
            response.setBeneficioPago(toInteger(result[9]));
            response.setFechaHoraPago(toInteger(result[10]));
            response.setDatosAdicionales(toInteger(result[11]));
            response.setConfirmMonto(toInteger(result[12]));
            response.setConfirmFecha(toInteger(result[13]));
            response.setConfirmCanal(toInteger(result[14]));
            response.setConsecPago(toInteger(result[15]));
            response.setSummary((String) result[16]);
            response.setGestionHistoricaAudiosId((Integer) result[17]);

            return response;
        } catch (NoResultException e) {
            return null;
        }
    }
}
