package com.grupo6.clinicalhistory.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Métricas de las operaciones GraphQL (query y mutation).
 * Exporta graphql_requests_total y graphql_request_duration con las etiquetas
 * operation y status, consultables en /actuator/prometheus.
 */
@Aspect
@Component
public class GraphqlMetricsAspect {

    private final MeterRegistry registry;

    public GraphqlMetricsAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(org.springframework.graphql.data.method.annotation.QueryMapping) "
            + "|| @annotation(org.springframework.graphql.data.method.annotation.MutationMapping)")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().getName();
        long start = System.nanoTime();
        String status = "success";
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            status = "error";
            throw ex;
        } finally {
            registry.counter("graphql_requests",
                    "operation", operation,
                    "status", status).increment();
            Timer.builder("graphql_request_duration")
                    .tags("operation", operation, "status", status)
                    .register(registry)
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }
}
