# Clinical History GraphQL

Servicio web para gestionar historias clínicas básicas de pacientes, desarrollado con el mismo framework del proyecto Bookly:

- Spring Boot 4.0.8 y Java 17
- Maven
- Spring for GraphQL
- PostgreSQL y Flyway
- Redis
- Docker Compose
- Prometheus y Grafana

## Alcance

La API implementará:

- Consulta de historias clínicas por cédula.
- Registro de una historia clínica.
- Relación entre pacientes, doctores e historias.
- Paginación.
- Caché local con Redis.
- Métricas y dashboard de observabilidad.

El API REST histórico de Bookly fue retirado porque no pertenece al alcance del parcial.

## Ejecución

    cp .env.example .env
    docker compose up -d --build

Servicios:

- GraphQL: http://localhost:8080/graphql
- GraphiQL: http://localhost:8080/graphiql
- Métricas: http://localhost:8081/actuator/prometheus
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

Pruebas:

    ./mvnw clean verify

## Estructura

    src/main/java/com/bookly/backendcf/
    └── clinicalhistory/
        ├── application/
        ├── domain/
        ├── infrastructure/
        └── presentation/graphql/

    src/main/resources/
    ├── graphql/schema.graphqls
    └── db/migration/V1__create_clinical_history.sql

## Trabajo colaborativo

Ramas de trabajo:

    integration/parcial-graphql
    ├── feature/01-infra-graphql-redis
    ├── feature/02-historias-persistencia
    ├── feature/03-historias-graphql
    └── feature/04-calidad-observabilidad

No se deben hacer commits directos sobre main.
