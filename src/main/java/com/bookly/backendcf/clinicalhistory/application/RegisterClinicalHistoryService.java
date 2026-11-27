package com.bookly.backendcf.clinicalhistory.application;

import com.bookly.backendcf.clinicalhistory.application.command.RegisterClinicalHistoryCommand;
import com.bookly.backendcf.clinicalhistory.application.port.out.ClinicalHistoryQueryCache;
import com.bookly.backendcf.clinicalhistory.application.port.out.ClinicalHistoryRepository;
import com.bookly.backendcf.clinicalhistory.application.port.out.DoctorRepository;
import com.bookly.backendcf.clinicalhistory.application.port.out.PatientRepository;
import com.bookly.backendcf.clinicalhistory.domain.ClinicalHistory;
import com.bookly.backendcf.clinicalhistory.domain.Doctor;
import com.bookly.backendcf.clinicalhistory.domain.Patient;
import com.bookly.backendcf.clinicalhistory.domain.exception.DomainValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Caso de uso de registro: valida la entrada, reutiliza paciente/doctor si ya
 * existen (cédula y cédula profesional son únicas) y persiste la historia.
 */
@Service
@Transactional
public class RegisterClinicalHistoryService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final ClinicalHistoryQueryCache cache;
    private final Validator validator;

    public RegisterClinicalHistoryService(PatientRepository patientRepository,
                                          DoctorRepository doctorRepository,
                                          ClinicalHistoryRepository clinicalHistoryRepository,
                                          ClinicalHistoryQueryCache cache,
                                          Validator validator) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.clinicalHistoryRepository = clinicalHistoryRepository;
        this.cache = cache;
        this.validator = validator;
    }

    public ClinicalHistory register(RegisterClinicalHistoryCommand command) {
        validate(command);

        RegisterClinicalHistoryCommand.PatientData patientData = command.patient();
        RegisterClinicalHistoryCommand.DoctorData doctorData = command.doctor();
        RegisterClinicalHistoryCommand.HistoryData historyData = command.history();

        Patient patient = patientRepository.findByIdentification(patientData.identification())
                .orElseGet(() -> patientRepository.save(Patient.create(
                        patientData.identification(),
                        patientData.firstName(),
                        patientData.lastName(),
                        patientData.age(),
                        patientData.gender())));

        Doctor doctor = doctorRepository.findByProfessionalLicense(doctorData.professionalLicense())
                .orElseGet(() -> doctorRepository.save(Doctor.create(
                        doctorData.name(),
                        doctorData.professionalLicense(),
                        doctorData.specialty())));

        ClinicalHistory history = ClinicalHistory.create(
                patient,
                doctor,
                historyData.consultationReason(),
                historyData.diagnosis(),
                historyData.treatment(),
                historyData.consultationDate());

        ClinicalHistory saved = clinicalHistoryRepository.save(history);

        // La respuesta cacheada de este paciente ya no es válida. Las consultas
        // de otros pacientes se dejan intactas.
        cache.invalidate(patientData.identification());

        return saved;
    }

    private void validate(RegisterClinicalHistoryCommand command) {
        if (command == null) {
            throw new DomainValidationException("La solicitud de registro es obligatoria");
        }
        Set<ConstraintViolation<RegisterClinicalHistoryCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            String detalle = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new DomainValidationException("Datos inválidos: " + detalle);
        }
    }
}
