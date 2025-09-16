package com.foh.contacto_total_web_service.acuerdos.infrastructure.persistence.jpa.repositories;

import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources.DatosAcuerdoResource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CartaAcuerdoRepository  {
    @PersistenceContext
    private EntityManager entityManager;

    public Optional<DatosAcuerdoResource> findByDniAndTramo(String dni, String tramo) {
        String jpql = """
        SELECT
        	CURDATE() AS FechaActual,
            GH.Cliente AS NombreDelTitular,
            TM.NUMCUENTAPMCP AS NroCuentaTarjetaOh,
            GH.FechaCompromiso,
            GH.DeudaTotal AS DeudaTotal,
            TM.SLDCAPITALASIG AS SaldoCapitalAsig,
            TM.`5` AS LTD,
            TM.LTDESPECIAL AS LTDEspecial,
            GH.UsuarioRegistra AS Asesor,
            GH.Observacion
        FROM `foh-prd`.GESTION_HISTORICA AS GH
        INNER JOIN TEMP_MERGE AS TM ON GH.Documento = TM.DOCUMENTO
        WHERE GH.Resultado IN ('PROMESA DE PAGO', 'OPORTUNIDAD DE PAGO')
            AND GH.Documento = ?1
            AND TM.RANGOMORAPROYAG = ?2
        """;

        try {
            Query query = entityManager.createNativeQuery(jpql)
                    .setParameter(1, dni)
                    .setParameter(2, tramo);

            Object resultObj = query.getSingleResult();

            if (!(resultObj instanceof Object[] result)) {
                throw new IllegalStateException("El resultado no es un arreglo de objetos");
            }

            DatosAcuerdoResource resource = new DatosAcuerdoResource(
                    result[0].toString(), // fechaActual
                    result[1].toString(), // nombreDelTitular
                    result[2].toString(), // cuentaTarjeta
                    result[3] != null ? result[3].toString() : null, // fechaCompromiso
                    result[4].toString(), // deudaTotal
                    result[5] != null ? result[5].toString() : null, // saldoCapitalAsig
                    result[6] != null ? result[6].toString() : null, // ltd,
                    result[7] != null ? result[7].toString() : null, // ltde
                    result[8].toString(), //asesor
                    result[9] != null ? result[9].toString() : null  // observacion
            );

            return Optional.of(resource);

        } catch (NoResultException e) {
            System.out.println("No se encontraron resultados para DNI: " + dni + ", tramo: " + tramo);
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace(); // imprime cualquier otro error
            throw e;
        }
    }
}
