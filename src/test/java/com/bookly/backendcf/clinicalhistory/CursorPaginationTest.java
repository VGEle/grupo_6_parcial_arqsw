package com.bookly.backendcf.clinicalhistory;

import com.bookly.backendcf.clinicalhistory.application.FindClinicalHistoriesService;
import com.bookly.backendcf.clinicalhistory.application.RegisterClinicalHistoryService;
import com.bookly.backendcf.clinicalhistory.application.command.RegisterClinicalHistoryCommand;
import com.bookly.backendcf.clinicalhistory.application.page.ClinicalHistoryPage;
import com.bookly.backendcf.clinicalhistory.application.page.Cursor;
import com.bookly.backendcf.clinicalhistory.domain.ClinicalHistory;
import com.bookly.backendcf.clinicalhistory.domain.Gender;
import com.bookly.backendcf.clinicalhistory.domain.exception.DomainValidationException;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CursorPaginationTest {

    private static final String CEDULA = "1234567890";
    private static final String IDENTIFICADOR = "LP-1234";

    @Autowired
    private RegisterClinicalHistoryService registerService;

    @Autowired
    private FindClinicalHistoriesService findService;

    @Autowired
    private GraphQlSource graphQlSource;

    @Test
    void elCursorApuntaAlUltimoRegistroReal() {
        ClinicalHistory oldest = registrar("2026-01-10T10:00:00Z", "Gripe-10");
        ClinicalHistory middle = registrar("2026-01-11T10:00:00Z", "Gripe-11");
        ClinicalHistory newest = registrar("2026-01-12T10:00:00Z", "Gripe-12");

        ClinicalHistoryPage firstPage = findService.findPage(CEDULA, 1, null);

        assertThat(firstPage.nodes()).containsExactly(newest);
        assertThat(firstPage.hasNextPage()).isTrue();
        assertThat(firstPage.endCursor()).isNotNull();

        Cursor cursor = Cursor.decode(firstPage.endCursor());
        assertThat(cursor.consultationDate()).isEqualTo(newest.getConsultationDate());
        assertThat(cursor.id()).isEqualTo(newest.getId());
        assertThat(cursor.id()).isNotEqualTo(middle.getId());
        assertThat(cursor.id()).isNotEqualTo(oldest.getId());
    }

    @Test
    void laSegundaPaginaContinuaDesdeElCursor() {
        registrar("2026-01-10T10:00:00Z", "Gripe-10");
        registrar("2026-01-11T10:00:00Z", "Gripe-11");
        registrar("2026-01-12T10:00:00Z", "Gripe-12");

        ClinicalHistoryPage firstPage = findService.findPage(CEDULA, 1, null);
        ClinicalHistoryPage secondPage =
                findService.findPage(CEDULA, 1, firstPage.endCursor());
        ClinicalHistoryPage thirdPage =
                findService.findPage(CEDULA, 1, secondPage.endCursor());

        assertThat(secondPage.nodes().get(0).getDiagnosis()).isEqualTo("Gripe-11");
        assertThat(secondPage.hasNextPage()).isTrue();
        assertThat(thirdPage.nodes().get(0).getDiagnosis()).isEqualTo("Gripe-10");
        assertThat(thirdPage.hasNextPage()).isFalse();
        assertThat(thirdPage.endCursor()).isNull();
    }

    @Test
    void elOrdenEsEstableCuandoSeRepiteLaFecha() {
        registrar("2026-01-12T10:00:00Z", "Misma-fecha-1");
        registrar("2026-01-12T10:00:00Z", "Misma-fecha-2");

        ClinicalHistoryPage page = findService.findPage(CEDULA, 2, null);
        List<UUID> ids = page.nodes().stream().map(ClinicalHistory::getId).toList();

        assertThat(ids).hasSize(2);
        assertThat(ids.get(0).compareTo(ids.get(1))).isPositive();
    }

    @Test
    void rechazaCursorInvalido() {
        assertThatThrownBy(() -> findService.findPage(CEDULA, 10, "!!!"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Cursor");
        assertThatThrownBy(() -> findService.findPage(CEDULA, 10, "cGFnZTox")) // cursor de página viejo
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rechazaFirstNegativoYMayorAlMaximo() {
        assertThatThrownBy(() -> findService.findPage(CEDULA, -1, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> findService.findPage(CEDULA, 51, null))
                .isInstanceOf(DomainValidationException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void laQueryGraphqlSigueFuncionandoConCursor() {
        registrar("2026-01-11T10:00:00Z", "Gripe-11");
        registrar("2026-01-12T10:00:00Z", "Gripe-12");

        ExecutionResult result = graphQlSource.graphQl().execute(ExecutionInput.newExecutionInput()
                .query("""
                        query Historias($cedula: String!, $first: Int, $after: String) {
                          clinicalHistoriesByIdentification(
                            identification: $cedula, first: $first, after: $after
                          ) {
                            nodes { id diagnosis }
                            pageInfo { hasNextPage endCursor }
                          }
                        }
                        """)
                .variables(Map.of("cedula", CEDULA, "first", 1))
                .build());

        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> connection =
                (Map<String, Object>) data.get("clinicalHistoriesByIdentification");
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) connection.get("nodes");
        Map<String, Object> pageInfo = (Map<String, Object>) connection.get("pageInfo");

        assertThat(nodes.get(0).get("diagnosis")).isEqualTo("Gripe-12");
        assertThat(pageInfo.get("hasNextPage")).isEqualTo(true);
        assertThat(pageInfo.get("endCursor")).isNotNull();
    }

    private ClinicalHistory registrar(String fecha, String diagnostico) {
        return registerService.register(new RegisterClinicalHistoryCommand(
                new RegisterClinicalHistoryCommand.PatientData(
                        CEDULA, "Ana", "Pérez", 30, Gender.FEMALE),
                new RegisterClinicalHistoryCommand.DoctorData(
                        "Dr. Casa", IDENTIFICADOR, "Medicina General"),
                new RegisterClinicalHistoryCommand.HistoryData(
                        "Dolor de cabeza", diagnostico, "Reposo", Instant.parse(fecha))));
    }
}
