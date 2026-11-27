package com.grupo6.clinicalhistory.application.page;

import com.grupo6.clinicalhistory.domain.exception.DomainValidationException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

/**
 * Cursor opaco que apunta a la posición exacta del último registro devuelto.
 * Codifica fecha de consulta e ID para mantener un orden estable (keyset
 * pagination), en lugar de codificar un número de página.
 */
public record Cursor(Instant consultationDate, UUID id) {

    private static final String PREFIX = "cursor:";
    private static final String SEPARATOR = "\\|";

    public static String encode(Cursor cursor) {
        String raw = PREFIX + cursor.consultationDate() + "|" + cursor.id();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Cursor de paginación inválido");
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            if (!decoded.startsWith(PREFIX)) {
                throw new IllegalArgumentException();
            }
            String[] parts = decoded.substring(PREFIX.length()).split(SEPARATOR, -1);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException();
            }
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw new DomainValidationException("Cursor de paginación inválido");
        }
    }
}
