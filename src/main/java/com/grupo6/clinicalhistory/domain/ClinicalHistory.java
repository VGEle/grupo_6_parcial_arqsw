package com.grupo6.clinicalhistory.domain;

import com.grupo6.clinicalhistory.domain.exception.DomainValidationException;
import com.grupo6.clinicalhistory.domain.validation.DomainRules;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Corresponde a la tabla clinical_histories del script V1__create_clinical_history.sql.
 * Patient 1:N ClinicalHistory y Doctor 1:N ClinicalHistory se modelan con
 * @ManyToOne desde esta entidad (lado propietario de las FK patient_id y doctor_id).
 */
@Entity
@Table(name = "clinical_histories")
public class ClinicalHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false, updatable = false)
    private Doctor doctor;

    @NotBlank
    @Size(max = DomainRules.MAX_CONSULTATION_REASON_LENGTH)
    @Column(name = "consultation_reason", nullable = false, length = 500)
    private String consultationReason;

    @NotBlank
    @Size(max = DomainRules.MAX_DIAGNOSIS_LENGTH)
    @Column(name = "diagnosis", nullable = false, length = 500)
    private String diagnosis;

    @NotBlank
    @Size(max = DomainRules.MAX_TREATMENT_LENGTH)
    @Column(name = "treatment", nullable = false, length = 1000)
    private String treatment;

    @NotNull
    @Column(name = "consultation_date", nullable = false)
    private Instant consultationDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClinicalHistory() {
        // requerido por JPA
    }

    public static ClinicalHistory create(Patient patient, Doctor doctor, String consultationReason,
                                         String diagnosis, String treatment, Instant consultationDate) {
        if (patient == null) {
            throw new DomainValidationException("El paciente es obligatorio");
        }
        if (doctor == null) {
            throw new DomainValidationException("El doctor es obligatorio");
        }
        if (consultationDate == null) {
            throw new DomainValidationException("La fecha de consulta es obligatoria");
        }
        ClinicalHistory history = new ClinicalHistory(
                UUID.randomUUID(),
                patient,
                doctor,
                DomainRules.requireText(consultationReason, "El motivo de consulta",
                        DomainRules.MAX_CONSULTATION_REASON_LENGTH),
                DomainRules.requireText(diagnosis, "El diagnóstico", DomainRules.MAX_DIAGNOSIS_LENGTH),
                DomainRules.requireText(treatment, "El tratamiento", DomainRules.MAX_TREATMENT_LENGTH),
                consultationDate,
                Instant.now());
        patient.addClinicalHistory(history);
        doctor.addClinicalHistory(history);
        return history;
    }

    public ClinicalHistory(UUID id, Patient patient, Doctor doctor, String consultationReason,
                           String diagnosis, String treatment, Instant consultationDate, Instant createdAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        if (patient == null) {
            throw new DomainValidationException("El paciente es obligatorio");
        }
        if (doctor == null) {
            throw new DomainValidationException("El doctor es obligatorio");
        }
        if (consultationDate == null) {
            throw new DomainValidationException("La fecha de consulta es obligatoria");
        }
        this.patient = patient;
        this.doctor = doctor;
        this.consultationReason = DomainRules.requireText(consultationReason, "El motivo de consulta",
                DomainRules.MAX_CONSULTATION_REASON_LENGTH);
        this.diagnosis = DomainRules.requireText(diagnosis, "El diagnóstico", DomainRules.MAX_DIAGNOSIS_LENGTH);
        this.treatment = DomainRules.requireText(treatment, "El tratamiento", DomainRules.MAX_TREATMENT_LENGTH);
        this.consultationDate = consultationDate;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public String getConsultationReason() {
        return consultationReason;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public Instant getConsultationDate() {
        return consultationDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
