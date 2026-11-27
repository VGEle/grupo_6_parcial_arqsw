package com.bookly.backendcf.clinicalhistory.domain.validation;

import com.bookly.backendcf.clinicalhistory.domain.Gender;
import com.bookly.backendcf.clinicalhistory.domain.exception.DomainValidationException;

import java.util.regex.Pattern;

/**
 * Reglas de validación del dominio. Los límites coinciden con el CHECK y con
 * los VARCHAR de V1__create_clinical_history.sql.
 */
public final class DomainRules {

    public static final int MAX_AGE = 150;
    public static final int MAX_IDENTIFICATION_LENGTH = 30;
    public static final int MAX_FIRST_NAME_LENGTH = 100;
    public static final int MAX_LAST_NAME_LENGTH = 100;
    public static final int MAX_DOCTOR_NAME_LENGTH = 200;
    public static final int MAX_LICENSE_LENGTH = 50;
    public static final int MAX_SPECIALTY_LENGTH = 150;
    public static final int MAX_CONSULTATION_REASON_LENGTH = 500;
    public static final int MAX_DIAGNOSIS_LENGTH = 500;
    public static final int MAX_TREATMENT_LENGTH = 1000;

    /**
     * Cédula/documento: alfanumérico con guiones, entre 4 y 30 caracteres.
     * El CHECK de la BD solo exige NOT NULL y VARCHAR(30); si se requiere un
     * formato país-específico (por ejemplo dígito verificador ecuatoriano),
     * éste es el único punto a modificar.
     */
    private static final Pattern IDENTIFICATION_PATTERN =
            Pattern.compile("^[A-Za-z0-9-]{4,30}$");

    private DomainRules() {
    }

    public static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field + " es obligatorio");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new DomainValidationException(field + " no puede superar " + maxLength + " caracteres");
        }
        return trimmed;
    }

    public static String validateIdentification(String identification) {
        String value = requireText(identification, "La cédula", MAX_IDENTIFICATION_LENGTH);
        if (!IDENTIFICATION_PATTERN.matcher(value).matches()) {
            throw new DomainValidationException(
                    "La cédula debe tener entre 4 y 30 caracteres alfanuméricos o guiones");
        }
        return value;
    }

    public static int validateAge(int age) {
        if (age < 0 || age > MAX_AGE) {
            throw new DomainValidationException("La edad debe estar entre 0 y " + MAX_AGE);
        }
        return age;
    }

    public static Gender validateGender(Gender gender) {
        if (gender == null) {
            throw new DomainValidationException("El género es obligatorio");
        }
        return gender;
    }
}
