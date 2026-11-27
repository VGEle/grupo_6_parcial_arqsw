# Evidencias de ejecución

Este documento organiza las evidencias de funcionamiento de la API GraphQL.
Las pruebas deben ejecutarse con datos ficticios y no deben incluir información
real de pacientes.

## Información de la ejecución

Completar estos datos cuando se realice la ejecución final:

| Campo | Valor |
|---|---|
| Fecha | `AAAA-MM-DD` |
| Rama | `feature/04-pruebas-documentacion` o rama de integración |
| Commit | `SHA del commit probado` |
| Comando de inicio | `docker compose up -d --build` |
| Resultado de pruebas | `./mvnw clean verify` |

## Evidencias requeridas

| ID | Evidencia | Estado | Archivo sugerido |
|---|---|---|---|
| E01 | Contenedores Docker activos | Completada | `evidence-01-docker.png` |
| E02 | Mutation de creación | Completada | `evidence-02-mutation.png` |
| E03 | Query por identificación | Completada | `evidence-03-query.png` |
| E04 | Paginación GraphQL | Completada | `evidence-04-pagination-page1.png`, `evidence-04-pagination-page2.png` |
| E05 | Validación de datos inválidos | Completada | `evidence-05-validation-error.png` |
| E06 | Redis cache miss | Completada | `evidence-06-redis-miss.png` |
| E07 | Redis cache hit | Completada | `evidence-07-redis-hit.png` |
| E08 | Invalidación de caché | Completada | `evidence-08-cache-invalidation.png` |
| E09 | Métricas en Prometheus | Completada | `evidence-09-prometheus-target.png`, `evidence-09-prometheus-graphql.png`, `evidence-09-prometheus-redis.png` |
| E10 | Dashboard de Grafana | Completada | `evidence-10-grafana-1.png`, `evidence-10-grafana-2.png` |
| E11 | Pruebas automatizadas | Completada | `evidence-11-tests.png` |

Las imágenes deben guardarse en:

```text
docs/evidence/
```

\* Estas evidencias fueron tomadas antes de reconstruir Docker con el commit
actual que incluye Redis, métricas y el cursor definitivo. Para la entrega
final deben repetirse después de ejecutar `docker compose up -d --build`.

## E01 — Contenedores Docker

Ejecutar:

```bash
docker compose up -d --build
docker compose ps
```

La evidencia debe mostrar los servicios activos:

- Aplicación.
- PostgreSQL.
- Redis.
- Prometheus.
- Grafana.

**Captura requerida:** salida de `docker compose ps` o Docker Desktop mostrando
los cinco servicios en ejecución.

## E02 — Mutation de creación

Abrir:

```text
http://localhost:8080/graphiql
```

Ejecutar la mutation documentada en `docs/GRAPHQL.md` utilizando datos ficticios.
La respuesta debe mostrar:

- ID de la historia creada.
- Datos del paciente.
- Datos del médico.
- Motivo de consulta.
- Diagnóstico.
- Tratamiento.
- Fecha.

**Captura requerida:** consulta, variables y respuesta `data` visible en
GraphiQL.

## E03 — Query por identificación

Ejecutar en GraphiQL:

```graphql
query {
  clinicalHistoriesByIdentification(
    identification: "1000000001"
    first: 10
  ) {
    nodes {
      id
      patient {
        identification
        firstName
        lastName
      }
      doctor {
        name
        specialty
      }
      diagnosis
      date
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

**Captura requerida:** query y respuesta con al menos una historia clínica.

## E04 — Paginación

Crear suficientes historias ficticias para obtener más de una página. Ejecutar
primero una consulta con un tamaño pequeño:

```graphql
query {
  clinicalHistoriesByIdentification(
    identification: "1000000001"
    first: 1
  ) {
    nodes {
      id
      date
      diagnosis
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

Luego utilizar el valor de `endCursor` en `after` para solicitar la siguiente
página:

```graphql
query {
  clinicalHistoriesByIdentification(
    identification: "1000000001"
    first: 1
    after: "<endCursor-anterior>"
  ) {
    nodes {
      id
      date
      diagnosis
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

**Captura requerida:** primera página y segunda página mostrando que el cursor
permite avanzar en los resultados.

## E05 — Validación de datos inválidos

Ejecutar una mutation con datos inválidos, por ejemplo una edad negativa o un
género que no exista en el enum:

```graphql
mutation {
  createClinicalHistory(
    input: {
      patient: {
        identification: "invalid-id"
        firstName: "Paciente"
        lastName: "Prueba"
        age: -1
        gender: OTHER
      }
      doctor: {
        name: "Doctor Prueba"
        professionalLicense: "MED-INVALID"
        specialty: "Pruebas"
      }
      history: {
        consultationReason: "Prueba de validación"
        diagnosis: "Datos inválidos"
        treatment: "Ninguno"
        date: "2026-11-27T14:00:00Z"
      }
    }
  ) {
    id
  }
}
```

**Captura requerida:** respuesta `errors` de GraphQL y mensaje de validación.

## E06, E07 y E08 — Redis

Estas evidencias deben tomarse después de que la Persona 3 termine la
implementación del caché.

### E06 — Cache miss

1. Limpiar el caché de desarrollo o utilizar una identificación que todavía no
   haya sido consultada.
2. Ejecutar la query por identificación.
3. Consultar las métricas de la aplicación.

```bash
curl -s http://localhost:8081/actuator/prometheus \
  | grep redis_cache_misses_total
```

**Captura requerida:** query ejecutada y métrica de cache miss incrementada.

### E07 — Cache hit

1. Ejecutar nuevamente la misma query con los mismos parámetros.
2. Consultar las métricas:

```bash
curl -s http://localhost:8081/actuator/prometheus \
  | grep redis_cache_hits_total
```

**Captura requerida:** segunda consulta y métrica de cache hit incrementada.

### E08 — Invalidación después de crear

1. Consultar las historias de un paciente.
2. Crear una nueva historia para el mismo paciente.
3. Repetir la consulta.
4. Confirmar que la nueva historia aparece y que se consultó nuevamente el
   origen de datos.

**Captura requerida:** mutation, consulta posterior y evidencia de invalidación.

## E09 — Prometheus

Abrir:

```text
http://localhost:9090
```

Comprobar que Prometheus tenga el target de la aplicación en estado `UP`.
También consultar las métricas requeridas:

```text
graphql_requests_total
graphql_request_duration
redis_cache_hits_total
redis_cache_misses_total
```

**Captura requerida:** pantalla de targets y una consulta de métrica exitosa.

## E10 — Grafana

Abrir:

```text
http://localhost:3000
```

Comprobar que el dashboard muestre información para:

- Peticiones GraphQL.
- Duración de peticiones.
- Errores.
- Cache hits.
- Cache misses.
- Estado de la aplicación.

**Captura requerida:** dashboard cargado con datos visibles, no solamente
paneles vacíos.

## E11 — Pruebas automatizadas

Ejecutar:

```bash
./mvnw clean verify
```

La evidencia debe mostrar:

- `BUILD SUCCESS`.
- Número de pruebas ejecutadas.
- Cero fallos.
- Cero errores.

**Captura requerida:** resultado final de la terminal o reporte generado por
Maven.

## Reglas para las capturas

- Utilizar únicamente datos ficticios.
- No mostrar contraseñas, tokens ni variables sensibles.
- Incluir la URL o el comando utilizado cuando sea relevante.
- Evitar capturas de pantallas vacías o que no muestren el resultado.
- Nombrar los archivos según la tabla de evidencias.
- Mantener la tabla de evidencias sincronizada con los archivos guardados en
  `docs/evidence/`.
