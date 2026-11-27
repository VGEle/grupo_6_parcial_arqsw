package com.bookly.backendcf.clinicalhistory.application;

import com.bookly.backendcf.clinicalhistory.application.port.out.ClinicalHistoryRepository;
import com.bookly.backendcf.clinicalhistory.domain.ClinicalHistory;
import com.bookly.backendcf.clinicalhistory.domain.exception.DomainValidationException;
import com.bookly.backendcf.clinicalhistory.domain.validation.DomainRules;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de consulta de historias clínicas.
 */
@Service
@Transactional(readOnly = true)
public class FindClinicalHistoriesService {

    private final ClinicalHistoryRepository clinicalHistoryRepository;

    public FindClinicalHistoriesService(ClinicalHistoryRepository clinicalHistoryRepository) {
        this.clinicalHistoryRepository = clinicalHistoryRepository;
    }

    /** Devuelve las historias de una cédula, la más reciente primero. */
    public Page<ClinicalHistory> findByIdentification(String identification, int page, int size) {
        String cedula = DomainRules.validateIdentification(identification);
        if (page < 0 || size < 1) {
            throw new DomainValidationException("La página debe ser >= 0 y el tamaño >= 1");
        }
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "consultationDate"));
        return clinicalHistoryRepository.findByPatientIdentification(cedula, pageable);
    }

    public Optional<ClinicalHistory> findById(UUID id) {
        if (id == null) {
            throw new DomainValidationException("El id de la historia es obligatorio");
        }
        return clinicalHistoryRepository.findById(id);
    }
}
