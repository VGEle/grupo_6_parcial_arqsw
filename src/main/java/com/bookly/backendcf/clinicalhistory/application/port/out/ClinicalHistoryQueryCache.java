package com.bookly.backendcf.clinicalhistory.application.port.out;

import java.util.Map;
import java.util.Optional;

/**
 * Caché de las respuestas de consulta de historias clínicas.
 * La clave debe considerar identificación, first y after.
 */
public interface ClinicalHistoryQueryCache {

    Optional<Map<String, Object>> get(String identification, Integer first, String after);

    void put(String identification, Integer first, String after, Map<String, Object> response);

    /** Invalida todas las variantes de consulta de un paciente. */
    void invalidate(String identification);
}
