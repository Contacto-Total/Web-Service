package com.foh.contacto_total_web_service.repository;

import com.foh.contacto_total_web_service.dto.TempMergeResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

@Repository
public class TempMergeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public TempMergeResponse getEmailAndTelefonoByDocumentoInTempMerge(String entidad, String documento) {
        String sql = "SELECT DOCUMENTO, EMAIL, TELEFONOCELULAR FROM TEMP_MERGE WHERE DOCUMENTO = ? AND ENTIDAD = ?";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, documento);
        query.setParameter(2, entidad);

        try {
            Object[] result = (Object[]) query.getSingleResult();

            TempMergeResponse response = new TempMergeResponse();
            response.setDOCUMENTO((String) result[0]);
            response.setEMAIL((String) result[1]);
            response.setTELEFONOCELULAR((String) result[2]);

            return response;
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        }
    }
}
