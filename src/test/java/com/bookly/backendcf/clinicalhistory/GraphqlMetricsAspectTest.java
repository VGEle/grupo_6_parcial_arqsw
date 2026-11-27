package com.bookly.backendcf.clinicalhistory;

import com.bookly.backendcf.clinicalhistory.infrastructure.metrics.GraphqlMetricsAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphqlMetricsAspectTest {

    @Test
    void registraContadorYTemporizadorConEtiquetas() throws Throwable {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        GraphqlMetricsAspect aspect = new GraphqlMetricsAspect(registry);
        ProceedingJoinPoint joinPoint = joinPoint("clinicalHistoriesByIdentification");
        when(joinPoint.proceed()).thenReturn(Map.of());

        aspect.measure(joinPoint);

        String scrape = registry.scrape();
        assertThat(scrape).contains("graphql_requests_total");
        assertThat(scrape).contains("operation=\"clinicalHistoriesByIdentification\"");
        assertThat(scrape).contains("status=\"success\"");
        assertThat(scrape).contains("graphql_request_duration_seconds_count");
    }

    @Test
    void registraLosErrores() throws Throwable {
        MeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        GraphqlMetricsAspect aspect = new GraphqlMetricsAspect(registry);
        ProceedingJoinPoint joinPoint = joinPoint("createClinicalHistory");
        when(joinPoint.proceed()).thenThrow(new RuntimeException("falló"));

        assertThatThrownBy(() -> aspect.measure(joinPoint))
                .isInstanceOf(RuntimeException.class);

        assertThat(((PrometheusMeterRegistry) registry).scrape())
                .contains("status=\"error\"")
                .contains("operation=\"createClinicalHistory\"");
    }

    private static ProceedingJoinPoint joinPoint(String operation) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(operation);
        return joinPoint;
    }
}
