package com.bookly.backendcf.clinicalhistory;

import com.bookly.backendcf.clinicalhistory.application.RegisterClinicalHistoryService;
import com.bookly.backendcf.clinicalhistory.application.command.RegisterClinicalHistoryCommand;
import com.bookly.backendcf.clinicalhistory.application.port.out.ClinicalHistoryQueryCache;
import com.bookly.backendcf.clinicalhistory.application.port.out.ClinicalHistoryRepository;
import com.bookly.backendcf.clinicalhistory.application.port.out.DoctorRepository;
import com.bookly.backendcf.clinicalhistory.application.port.out.PatientRepository;
import com.bookly.backendcf.clinicalhistory.domain.ClinicalHistory;
import com.bookly.backendcf.clinicalhistory.domain.Doctor;
import com.bookly.backendcf.clinicalhistory.domain.Gender;
import com.bookly.backendcf.clinicalhistory.domain.Patient;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheInvalidationOnCreateTest {

    private PatientRepository patients;
    private DoctorRepository doctors;
    private ClinicalHistoryRepository histories;
    private ClinicalHistoryQueryCache cache;
    private RegisterClinicalHistoryService service;

    @BeforeEach
    void setUp() {
        patients = mock(PatientRepository.class);
        doctors = mock(DoctorRepository.class);
        histories = mock(ClinicalHistoryRepository.class);
        cache = mock(ClinicalHistoryQueryCache.class);
        Validator validator = mock(Validator.class);
        when(validator.validate(any())).thenReturn(Set.of());

        when(patients.findByIdentification("1234567890")).thenReturn(Optional.empty());
        when(patients.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(doctors.findByProfessionalLicense("LP-1234")).thenReturn(Optional.empty());
        when(doctors.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(histories.save(any(ClinicalHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new RegisterClinicalHistoryService(
                patients, doctors, histories, cache, validator);
    }

    @Test
    void crearHistoriaInvalidaSoloLaCedulaDelPaciente() {
        ClinicalHistory guardada = service.register(command("1234567890"));

        assertThat(guardada).isNotNull();
        verify(cache).invalidate("1234567890");
        verify(cache, never()).invalidate("9999999999");
    }

    @Test
    void siElPacienteYaExisteSeInvalidaIgualmente() {
        Patient existente = Patient.create("1234567890", "Ana", "Pérez", 30, Gender.FEMALE);
        when(patients.findByIdentification("1234567890")).thenReturn(Optional.of(existente));

        service.register(command("1234567890"));

        verify(cache).invalidate("1234567890");
        verify(patients, never()).save(any(Patient.class));
    }

    private static RegisterClinicalHistoryCommand command(String cedula) {
        return new RegisterClinicalHistoryCommand(
                new RegisterClinicalHistoryCommand.PatientData(
                        cedula, "Ana", "Pérez", 30, Gender.FEMALE),
                new RegisterClinicalHistoryCommand.DoctorData(
                        "Dr. Casa", "LP-1234", "Medicina General"),
                new RegisterClinicalHistoryCommand.HistoryData(
                        "Dolor", "Gripe", "Reposo",
                        Instant.parse("2026-01-15T10:00:00Z")));
    }
}
