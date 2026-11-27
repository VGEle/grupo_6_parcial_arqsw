package com.grupo6.clinicalhistory.domain;

import com.grupo6.clinicalhistory.domain.exception.DomainValidationException;
import com.grupo6.clinicalhistory.domain.validation.DomainRules;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Corresponde a la tabla patients del script V1__create_clinical_history.sql.
 */
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9-]{4,30}$",
            message = "La cédula debe tener entre 4 y 30 caracteres alfanuméricos o guiones")
    @Size(max = DomainRules.MAX_IDENTIFICATION_LENGTH)
    @Column(name = "identification", nullable = false, length = 30, unique = true, updatable = false)
    private String identification;

    @NotBlank
    @Size(max = DomainRules.MAX_FIRST_NAME_LENGTH)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Size(max = DomainRules.MAX_LAST_NAME_LENGTH)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Min(0)
    @Max(DomainRules.MAX_AGE)
    @Column(name = "age", nullable = false)
    private int age;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Relación 1:N con clinical_histories. El dueño de la FK es ClinicalHistory. */
    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    private List<ClinicalHistory> clinicalHistories = new ArrayList<>();

    protected Patient() {
        // requerido por JPA
    }

    public static Patient create(String identification, String firstName, String lastName,
                                 int age, Gender gender) {
        return new Patient(
                UUID.randomUUID(),
                DomainRules.validateIdentification(identification),
                DomainRules.requireText(firstName, "El nombre", DomainRules.MAX_FIRST_NAME_LENGTH),
                DomainRules.requireText(lastName, "El apellido", DomainRules.MAX_LAST_NAME_LENGTH),
                DomainRules.validateAge(age),
                DomainRules.validateGender(gender),
                Instant.now());
    }

    public Patient(UUID id, String identification, String firstName, String lastName,
                   int age, Gender gender, Instant createdAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.identification = DomainRules.validateIdentification(identification);
        this.firstName = DomainRules.requireText(firstName, "El nombre", DomainRules.MAX_FIRST_NAME_LENGTH);
        this.lastName = DomainRules.requireText(lastName, "El apellido", DomainRules.MAX_LAST_NAME_LENGTH);
        this.age = DomainRules.validateAge(age);
        this.gender = DomainRules.validateGender(gender);
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

    public String getIdentification() {
        return identification;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getAge() {
        return age;
    }

    public Gender getGender() {
        return gender;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ClinicalHistory> getClinicalHistories() {
        return clinicalHistories;
    }
}
