package com.bookly.backendcf.clinicalhistory.domain;

/**
 * Género permitido por la migración V1__create_clinical_history.sql.
 * Se persiste como VARCHAR(20) mediante {@code @Enumerated(EnumType.STRING)}.
 */
public enum Gender {
    MALE,
    FEMALE,
    OTHER
}
