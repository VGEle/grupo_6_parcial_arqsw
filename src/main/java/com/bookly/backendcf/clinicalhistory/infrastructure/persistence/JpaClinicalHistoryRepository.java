package com.bookly.backendcf.clinicalhistory.infrastructure.persistence;

import com.bookly.backendcf.clinicalhistory.application.port.out.ClinicalHistoryRepository;
import com.bookly.backendcf.clinicalhistory.domain.ClinicalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio JPA de clinical_histories. Implementa el puerto de salida de aplicación.
 */
@Repository
public interface JpaClinicalHistoryRepository
        extends JpaRepository<ClinicalHistory, UUID>, ClinicalHistoryRepository {

    @Override
    @Query(value = "select h from ClinicalHistory h join h.patient p "
            + "where p.identification = :identification",
            countQuery = "select count(h) from ClinicalHistory h join h.patient p "
                    + "where p.identification = :identification")
    Page<ClinicalHistory> findByPatientIdentification(
            @Param("identification") String identification, Pageable pageable);
}
