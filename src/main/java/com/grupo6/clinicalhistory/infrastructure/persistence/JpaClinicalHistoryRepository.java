package com.grupo6.clinicalhistory.infrastructure.persistence;

import com.grupo6.clinicalhistory.application.port.out.ClinicalHistoryRepository;
import com.grupo6.clinicalhistory.domain.ClinicalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    @Override
    @Query(value = "select h from ClinicalHistory h join h.patient p "
            + "where p.identification = :identification "
            + "and (h.consultationDate < :cursorDate "
            + "or (h.consultationDate = :cursorDate and h.id < :cursorId)) "
            + "order by h.consultationDate desc, h.id desc",
            countQuery = "select count(h) from ClinicalHistory h join h.patient p "
                    + "where p.identification = :identification "
                    + "and (h.consultationDate < :cursorDate "
                    + "or (h.consultationDate = :cursorDate and h.id < :cursorId))")
    Slice<ClinicalHistory> findPageAfter(
            @Param("identification") String identification,
            @Param("cursorDate") Instant cursorDate,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
