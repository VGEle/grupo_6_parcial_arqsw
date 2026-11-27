package com.bookly.backendcf.clinicalhistory.infrastructure.cache;

import com.bookly.backendcf.clinicalhistory.application.port.out.ClinicalHistoryQueryCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Caché de consultas en Redis. Si Redis no está disponible, registra una
 * advertencia una sola vez y la aplicación sigue funcionando consultando la BD.
 */
@Slf4j
@Component
public class RedisClinicalHistoryQueryCache implements ClinicalHistoryQueryCache {

    static final String QUERY_PREFIX = "clinical-history:query:";
    static final String INDEX_PREFIX = "clinical-history:keys:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration ttl;
    private final AtomicBoolean warningLogged = new AtomicBoolean();

    public RedisClinicalHistoryQueryCache(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${clinical-history.cache.ttl-minutes:5}") long ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> get(
            String identification, Integer first, String after) {
        try {
            Object value = redisTemplate.opsForValue().get(key(identification, first, after));
            if (value instanceof Map<?, ?> map) {
                return Optional.of((Map<String, Object>) map);
            }
        } catch (RuntimeException ex) {
            warn("No se pudo leer la caché", ex);
        }
        return Optional.empty();
    }

    @Override
    public void put(String identification, Integer first, String after,
                    Map<String, Object> response) {
        try {
            String key = key(identification, first, after);
            redisTemplate.opsForValue().set(key, response, ttl);
            String indexKey = indexKey(identification);
            redisTemplate.opsForSet().add(indexKey, key);
            redisTemplate.expire(indexKey, ttl);
        } catch (RuntimeException ex) {
            warn("No se pudo escribir la caché", ex);
        }
    }

    @Override
    public void invalidate(String identification) {
        try {
            String indexKey = indexKey(identification);
            Set<Object> keys = redisTemplate.opsForSet().members(indexKey);
            if (keys != null && !keys.isEmpty()) {
                Set<String> toDelete = keys.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toSet());
                redisTemplate.delete(toDelete);
            }
            redisTemplate.delete(indexKey);
        } catch (RuntimeException ex) {
            warn("No se pudo invalidar la caché", ex);
        }
    }

    private String key(String identification, Integer first, String after) {
        return QUERY_PREFIX + identification
                + ":" + (first == null ? "default" : first)
                + ":" + (after == null ? "start" : after);
    }

    private String indexKey(String identification) {
        return INDEX_PREFIX + identification;
    }

    private void warn(String message, RuntimeException ex) {
        if (warningLogged.compareAndSet(false, true)) {
            log.warn("{}: Redis no disponible, se continúa sin caché ({})", message, ex.getMessage());
        }
    }
}
