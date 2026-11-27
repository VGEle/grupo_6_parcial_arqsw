package com.bookly.backendcf.clinicalhistory.domain;

import com.bookly.backendcf.clinicalhistory.domain.exception.DomainValidationException;
import com.bookly.backendcf.clinicalhistory.domain.validation.DomainRules;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Corresponde a la tabla doctors del script V1__create_clinical_history.sql.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Size(max = DomainRules.MAX_DOCTOR_NAME_LENGTH)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotBlank
    @Size(max = DomainRules.MAX_LICENSE_LENGTH)
    @Column(name = "professional_license", nullable = false, length = 50, unique = true, updatable = false)
    private String professionalLicense;

    @NotBlank
    @Size(max = DomainRules.MAX_SPECIALTY_LENGTH)
    @Column(name = "specialty", nullable = false, length = 150)
    private String specialty;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Relación 1:N con clinical_histories. El dueño de la FK es ClinicalHistory. */
    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
    private List<ClinicalHistory> clinicalHistories = new ArrayList<>();

    protected Doctor() {
        // requerido por JPA
    }

    public static Doctor create(String name, String professionalLicense, String specialty) {
        return new Doctor(
                UUID.randomUUID(),
                DomainRules.requireText(name, "El nombre del doctor", DomainRules.MAX_DOCTOR_NAME_LENGTH),
                DomainRules.requireText(professionalLicense, "La cédula profesional", DomainRules.MAX_LICENSE_LENGTH),
                DomainRules.requireText(specialty, "La especialidad", DomainRules.MAX_SPECIALTY_LENGTH),
                Instant.now());
    }

    public Doctor(UUID id, String name, String professionalLicense, String specialty, Instant createdAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.name = DomainRules.requireText(name, "El nombre del doctor", DomainRules.MAX_DOCTOR_NAME_LENGTH);
        this.professionalLicense = DomainRules.requireText(
                professionalLicense, "La cédula profesional", DomainRules.MAX_LICENSE_LENGTH);
        this.specialty = DomainRules.requireText(specialty, "La especialidad", DomainRules.MAX_SPECIALTY_LENGTH);
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public void addClinicalHistory(ClinicalHistory history) {
        if (history == null) {
            throw new DomainValidationException("La historia clínica es obligatoria");
        }
        if (!clinicalHistories.contains(history)) {
            clinicalHistories.add(history);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProfessionalLicense() {
        return professionalLicense;
    }

    public String getSpecialty() {
        return specialty;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ClinicalHistory> getClinicalHistories() {
        return clinicalHistories;
    }
}
