package com.grupo6.clinicalhistory.application.port.out;

import com.grupo6.clinicalhistory.domain.ClinicalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ClinicalHistoryRepository {

    ClinicalHistory save(ClinicalHistory clinicalHistory);

    Optional<ClinicalHistory> findById(UUID id);

    /**
     * Consulta paginada por cédula del paciente, ordenada según el Pageable
     * recibido (consultation_date DESC, id DESC).
     */
    Page<ClinicalHistory> findByPatientIdentification(String identification, Pageable pageable);

    /**
     * Keyset pagination: devuelve los registros estrictamente posteriores al
     * cursor (fecha de consulta e ID) en orden descendente estable.
     */
    Slice<ClinicalHistory> findPageAfter(String identification, Instant consultationDate,
                                         UUID id, Pageable pageable);
}
