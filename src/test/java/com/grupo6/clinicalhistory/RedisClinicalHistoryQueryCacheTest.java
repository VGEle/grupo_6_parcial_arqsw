package com.grupo6.clinicalhistory;

import com.grupo6.clinicalhistory.infrastructure.cache.RedisClinicalHistoryQueryCache;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisClinicalHistoryQueryCacheTest {

    private static final String CEDULA = "1234567890";
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> values;
    private SetOperations<String, Object> sets;
    private PrometheusMeterRegistry registry;
    private RedisClinicalHistoryQueryCache cache;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        values = mock(ValueOperations.class);
        sets = mock(SetOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(redisTemplate.opsForSet()).thenReturn(sets);
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        cache = new RedisClinicalHistoryQueryCache(redisTemplate, 5, registry);
    }

    @Test
    void hitDevuelveLaRespuestaCacheada() {
        Map<String, Object> response = Map.of("nodes", List.of());
        when(values.get("clinical-history:query:1234567890:10:start")).thenReturn(response);

        assertThat(cache.get(CEDULA, 10, null)).contains(response);
        assertThat(registry.get("redis_cache_hits").counter().count()).isEqualTo(1);
        assertThat(registry.scrape()).contains("redis_cache_hits_total");
    }

    @Test
    void missCuandoNoHayValor() {
        assertThat(cache.get(CEDULA, 10, null)).isEmpty();
        assertThat(registry.get("redis_cache_misses").counter().count()).isEqualTo(1);
        assertThat(registry.scrape()).contains("redis_cache_misses_total");
    }

    @Test
    void laClaveIncluyeIdentificacionFirstYAfter() {
        Map<String, Object> response = Map.of("nodes", List.of());
        String key = "clinical-history:query:1234567890:2:cursorABC";

        cache.put(CEDULA, 2, "cursorABC", response);

        verify(values).set(key, response, Duration.ofMinutes(5));
        verify(sets).add("clinical-history:keys:1234567890", key);
        verify(redisTemplate).expire("clinical-history:keys:1234567890", Duration.ofMinutes(5));
    }

    @Test
    void invalidacionAfectaSoloAlPaciente() {
        when(sets.members("clinical-history:keys:1234567890"))
                .thenReturn(Set.of("key-a", "key-b"));

        cache.invalidate(CEDULA);

        verify(redisTemplate).delete(Set.of("key-a", "key-b"));
        verify(redisTemplate).delete("clinical-history:keys:1234567890");
        verify(redisTemplate, never()).delete("clinical-history:keys:9999999999");
    }

    @Test
    void redisCaidoNoRompeLaAplicacion() {
        when(redisTemplate.opsForValue())
                .thenThrow(new RedisConnectionFailureException("Redis caído"));

        assertThat(cache.get(CEDULA, 10, null)).isEmpty();
        assertThat(registry.get("redis_cache_misses").counter().count()).isEqualTo(1);
        assertThatCode(() -> cache.put(CEDULA, 10, null, Map.of()))
                .doesNotThrowAnyException();
        assertThatCode(() -> cache.invalidate(CEDULA))
                .doesNotThrowAnyException();
    }
}
