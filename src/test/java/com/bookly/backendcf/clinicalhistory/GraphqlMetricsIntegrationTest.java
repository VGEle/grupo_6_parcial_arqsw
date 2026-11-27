package com.bookly.backendcf.clinicalhistory;

import graphql.ExecutionInput;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** Comprueba que el aspecto se aplica realmente a los resolvers GraphQL. */
@SpringBootTest
@Transactional
class GraphqlMetricsIntegrationTest {

    @Autowired
    private GraphQlSource graphQlSource;

    @Autowired
    private MeterRegistry registry;

    @Test
    void unaQueryRegistraContadorYTiempo() {
        graphQlSource.graphQl().execute(ExecutionInput.newExecutionInput()
                .query("""
                        { clinicalHistoriesByIdentification(identification: "1234567890") {
                            pageInfo { hasNextPage }
                        } }
                        """)
                .build());

        assertThat(registry.get("graphql_requests")
                .tag("operation", "clinicalHistoriesByIdentification")
                .tag("status", "success")
                .counter().count()).isGreaterThan(0);
        assertThat(registry.get("graphql_request_duration")
                .tag("operation", "clinicalHistoriesByIdentification")
                .timer().count()).isGreaterThan(0);
    }
}
