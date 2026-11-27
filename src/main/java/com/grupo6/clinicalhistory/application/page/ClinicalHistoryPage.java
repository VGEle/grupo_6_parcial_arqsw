package com.grupo6.clinicalhistory.application.page;

import com.grupo6.clinicalhistory.domain.ClinicalHistory;

import java.util.List;

/** Resultado de una página de historias clínicas con paginación por cursor. */
public record ClinicalHistoryPage(
        List<ClinicalHistory> nodes,
        boolean hasNextPage,
        String endCursor) {
}
