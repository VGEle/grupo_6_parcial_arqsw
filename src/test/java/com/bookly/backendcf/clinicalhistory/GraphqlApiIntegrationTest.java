package com.bookly.backendcf.clinicalhistory;

import com.bookly.backendcf.clinicalhistory.application.RegisterClinicalHistoryService;
import com.bookly.backendcf.clinicalhistory.application.command.RegisterClinicalHistoryCommand;
import com.bookly.backendcf.clinicalhistory.domain.Gender;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración del contrato GraphQL, independientes de GraphiQL.
 */
@SpringBootTest
@Transactional
class GraphqlApiIntegrationTest {

    @Autowired
    private GraphQlSource graphQlSource;

    @Autowired
    private RegisterClinicalHistoryService registerService;

    @Test
    void mutationRegistraUnaHistoriaConPacienteYDoctor() {
        ExecutionResult result = execute("""
                mutation {
                  createClinicalHistory(input: {
                    patient: {
                      identification: "9000000001"
                      firstName: "Paciente"
                      lastName: "GraphQL"
                      age: 28
                      gender: FEMALE
                    }
                    doctor: {
                      name: "Doctora GraphQL"
                      professionalLicense: "GQL-1001"
                      specialty: "Medicina general"
                    }
                    history: {
                      consultationReason: "Prueba GraphQL"
                      diagnosis: "Historia registrada"
                      treatment: "Seguimiento"
                      date: "2026-02-01T10:00:00Z"
                    }
                  }) {
                    id
                    patient { identification firstName lastName }
                    doctor { professionalLicense specialty }
                    diagnosis
                    date
                  }
                }
                """);

        assertThat(result.getErrors()).isEmpty();
        Map<?, ?> data = result.getData();
        Map<?, ?> history = (Map<?, ?>) data.get("createClinicalHistory");

        assertThat(history.get("id")).isNotNull();
        assertThat(((Map<?, ?>) history.get("patient")).get("identification"))
                .isEqualTo("9000000001");
        assertThat(((Map<?, ?>) history.get("doctor")).get("professionalLicense"))
                .isEqualTo("GQL-1001");
        assertThat(history.get("diagnosis")).isEqualTo("Historia registrada");
    }

    @Test
    void queryDevuelveHistoriasPorCedulaConPacienteYDoctor() {
        registerService.register(command("9000000002", "GQL-1002",
                "2026-02-02T10:00:00Z", "Consulta encontrada"));

        ExecutionResult result = execute("""
                query {
                  clinicalHistoriesByIdentification(
                    identification: "9000000002"
                    first: 10
                  ) {
                    nodes {
                      diagnosis
                      patient { identification }
                      doctor { professionalLicense }
                    }
                    pageInfo { hasNextPage endCursor }
                  }
                }
                """);

        assertThat(result.getErrors()).isEmpty();
        Map<?, ?> data = result.getData();
        Map<?, ?> connection = (Map<?, ?>) data.get("clinicalHistoriesByIdentification");
        List<?> nodes = (List<?>) connection.get("nodes");
        Map<?, ?> node = (Map<?, ?>) nodes.get(0);

        assertThat(nodes).hasSize(1);
        assertThat(node.get("diagnosis")).isEqualTo("Consulta encontrada");
        assertThat(((Map<?, ?>) node.get("patient")).get("identification"))
                .isEqualTo("9000000002");
        assertThat(((Map<?, ?>) node.get("doctor")).get("professionalLicense"))
                .isEqualTo("GQL-1002");
    }

    @Test
    void queryDevuelveErrorCuandoLaCedulaNoExiste() {
        ExecutionResult result = execute("""
                query {
                  clinicalHistoriesByIdentification(
                    identification: "9999999999"
                    first: 10
                  ) {
                    nodes { id }
                    pageInfo { hasNextPage endCursor }
                  }
                }
                """);

        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getMessage())
                .contains("No existe un paciente con cédula");
    }

    @Test
    void mutationDevuelveErrorCuandoLosDatosSonInvalidos() {
        ExecutionResult result = execute("""
                mutation {
                  createClinicalHistory(input: {
                    patient: {
                      identification: "9000000003"
                      firstName: "Paciente"
                      lastName: "Invalido"
                      age: -1
                      gender: FEMALE
                    }
                    doctor: {
                      name: "Doctor Prueba"
                      professionalLicense: "GQL-1003"
                      specialty: "Pruebas"
                    }
                    history: {
                      consultationReason: "Prueba inválida"
                      diagnosis: "Edad inválida"
                      treatment: "Ninguno"
                      date: "2026-02-03T10:00:00Z"
                    }
                  }) {
                    id
                  }
                }
                """);

        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getMessage())
                .contains("age");
    }

    @Test
    void mutationReutilizaPacienteYDoctorParaUnaNuevaHistoria() {
        execute(createMutation("9000000004", "GQL-1004",
                "2026-02-04T10:00:00Z", "Primera historia"));
        ExecutionResult second = execute(createMutation("9000000004", "GQL-1004",
                "2026-02-05T10:00:00Z", "Segunda historia"));

        assertThat(second.getErrors()).isEmpty();

        ExecutionResult query = execute("""
                query {
                  clinicalHistoriesByIdentification(
                    identification: "9000000004"
                    first: 10
                  ) {
                    nodes { diagnosis }
                  }
                }
                """);

        Map<?, ?> data = query.getData();
        Map<?, ?> connection = (Map<?, ?>) data.get("clinicalHistoriesByIdentification");
        assertThat((List<?>) connection.get("nodes")).hasSize(2);
    }

    private ExecutionResult execute(String query) {
        return graphQlSource.graphQl().execute(
                ExecutionInput.newExecutionInput().query(query).build());
    }

    private static String createMutation(String identification, String license,
                                         String date, String diagnosis) {
        return """
                mutation {
                  createClinicalHistory(input: {
                    patient: {
                      identification: "%s"
                      firstName: "Paciente"
                      lastName: "Prueba"
                      age: 30
                      gender: FEMALE
                    }
                    doctor: {
                      name: "Doctor Prueba"
                      professionalLicense: "%s"
                      specialty: "Medicina general"
                    }
                    history: {
                      consultationReason: "Consulta de prueba"
                      diagnosis: "%s"
                      treatment: "Seguimiento"
                      date: "%s"
                    }
                  }) { id }
                }
                """.formatted(identification, license, diagnosis, date);
    }

    private static RegisterClinicalHistoryCommand command(String identification,
                                                           String license,
                                                           String date,
                                                           String diagnosis) {
        return new RegisterClinicalHistoryCommand(
                new RegisterClinicalHistoryCommand.PatientData(
                        identification, "Paciente", "Prueba", 30, Gender.FEMALE),
                new RegisterClinicalHistoryCommand.DoctorData(
                        "Doctor Prueba", license, "Medicina general"),
                new RegisterClinicalHistoryCommand.HistoryData(
                        "Consulta de prueba", diagnosis, "Seguimiento", Instant.parse(date)));
    }
}
