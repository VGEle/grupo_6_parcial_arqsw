package com.bookly.backendcf.clinicalhistory.domain.exception;

/**
 * Se lanza cuando una regla de negocio o validación de entrada no se cumple.
 */
public class DomainValidationException extends RuntimeException {

    public DomainValidationException(String message) {
        super(message);
    }
}
