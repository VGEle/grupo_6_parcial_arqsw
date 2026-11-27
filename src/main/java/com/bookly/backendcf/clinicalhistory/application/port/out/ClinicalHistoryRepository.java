package com.bookly.backendcf.clinicalhistory.application.port.out;

import com.bookly.backendcf.clinicalhistory.domain.ClinicalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ClinicalHistoryRepository {

    ClinicalHistory save(ClinicalHistory clinicalHistory);

    Optional<ClinicalHistory> findById(UUID id);

    /**
     * Consulta paginada por cédula del paciente, ordenada según el Pageable
     * recibido (consultation_date DESC por defecto en el servicio).
     */
    Page<ClinicalHistory> findByPatientIdentification(String identification, Pageable pageable);
}
