package com.bookly.backendcf.clinicalhistory.application.command;

import com.bookly.backendcf.clinicalhistory.domain.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Datos de entrada para registrar una historia clínica. Las longitudes máximas
 * coinciden con los VARCHAR de V1__create_clinical_history.sql.
 */
public record RegisterClinicalHistoryCommand(
        @Valid @NotNull PatientData patient,
        @Valid @NotNull DoctorData doctor,
        @Valid @NotNull HistoryData history) {

    public record PatientData(
            @NotBlank
            @Pattern(regexp = "^[A-Za-z0-9-]{4,30}$",
                    message = "La cédula debe tener entre 4 y 30 caracteres alfanuméricos o guiones")
            @Size(max = 30) String identification,

            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,

            @Min(0) @Max(150) int age,

            @NotNull Gender gender) {
    }

    public record DoctorData(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 50) String professionalLicense,
            @NotBlank @Size(max = 150) String specialty) {
    }

    public record HistoryData(
            @NotBlank @Size(max = 500) String consultationReason,
            @NotBlank @Size(max = 500) String diagnosis,
            @NotBlank @Size(max = 1000) String treatment,
            @NotNull Instant consultationDate) {
    }
}
