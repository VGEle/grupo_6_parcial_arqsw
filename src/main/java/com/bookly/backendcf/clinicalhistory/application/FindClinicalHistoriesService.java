package com.bookly.backendcf.clinicalhistory.application;

import com.bookly.backendcf.clinicalhistory.application.page.ClinicalHistoryPage;
import com.bookly.backendcf.clinicalhistory.application.page.Cursor;
import com.bookly.backendcf.clinicalhistory.application.port.out.ClinicalHistoryRepository;
import com.bookly.backendcf.clinicalhistory.application.port.out.PatientRepository;
import com.bookly.backendcf.clinicalhistory.domain.ClinicalHistory;
import com.bookly.backendcf.clinicalhistory.domain.exception.DomainValidationException;
import com.bookly.backendcf.clinicalhistory.domain.validation.DomainRules;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de consulta de historias clínicas.
 */
@Service
@Transactional(readOnly = true)
public class FindClinicalHistoriesService {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;

    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final PatientRepository patientRepository;

    public FindClinicalHistoriesService(
            ClinicalHistoryRepository clinicalHistoryRepository,
            PatientRepository patientRepository) {
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Paginación por cursor: el cursor apunta al último registro real devuelto
     * (fecha de consulta e ID), no a un número de página.
     */
    public ClinicalHistoryPage findPage(String identification, Integer first, String after) {
        String cedula = DomainRules.validateIdentification(identification);
        int size = first == null ? DEFAULT_PAGE_SIZE : first;
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainValidationException(
                    "first debe estar entre 1 y " + MAX_PAGE_SIZE);
        }

        Cursor cursor = after == null || after.isBlank() ? null : Cursor.decode(after);
        ensurePatientExists(cedula);
        Sort sort = Sort.by(
                Sort.Order.desc("consultationDate"),
                Sort.Order.desc("id"));

        // Se pide un registro extra para saber si existe la siguiente página.
        Slice<ClinicalHistory> slice = cursor == null
                ? clinicalHistoryRepository.findByPatientIdentification(
                        cedula, PageRequest.of(0, size + 1, sort))
                : clinicalHistoryRepository.findPageAfter(
                        cedula, cursor.consultationDate(), cursor.id(),
                        PageRequest.of(0, size + 1, sort));

        List<ClinicalHistory> content = slice.getContent();
        boolean hasNextPage = content.size() > size;
        List<ClinicalHistory> nodes = hasNextPage ? content.subList(0, size) : content;

        String endCursor = null;
        if (hasNextPage && !nodes.isEmpty()) {
            ClinicalHistory last = nodes.get(nodes.size() - 1);
            endCursor = Cursor.encode(new Cursor(last.getConsultationDate(), last.getId()));
        }
        return new ClinicalHistoryPage(nodes, hasNextPage, endCursor);
    }

    /**
     * Consulta paginada por número de página (mantenida por compatibilidad).
     * Los registros más recientes aparecen primero.
     */
    public Page<ClinicalHistory> findByIdentification(
            String identification, int page, int size) {

        String cedula = DomainRules.validateIdentification(identification);
        ensurePatientExists(cedula);

        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainValidationException(
                    "La página debe ser >= 0 y el tamaño debe estar entre 1 y " + MAX_PAGE_SIZE);
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("consultationDate"),
                        Sort.Order.desc("id")
                )
        );

        return clinicalHistoryRepository.findByPatientIdentification(cedula, pageable);
    }

    private void ensurePatientExists(String identification) {
        if (patientRepository.findByIdentification(identification).isEmpty()) {
            throw new DomainValidationException(
                    "No existe un paciente con cédula: " + identification);
        }
    }

    public Optional<ClinicalHistory> findById(UUID id) {

        if (id == null) {
            throw new DomainValidationException(
                    "El id de la historia es obligatorio");
        }

        return clinicalHistoryRepository.findById(id);
    }
}
