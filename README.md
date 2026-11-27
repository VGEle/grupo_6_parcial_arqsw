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

## Pruebas

    .\mvnw clean verify

En Linux/Mac usar `./mvnw clean verify`. No hace falta levantar PostgreSQL ni Redis: las pruebas usan H2 en memoria.

- Flyway ejecuta `V1__create_clinical_history.sql`.
- Hibernate arranca con `spring.jpa.hibernate.ddl-auto=validate`, por lo que el contexto solo inicia si las entidades coinciden con el SQL de la migración.
- Se cubren registro y consulta paginada por cédula, reutilización de paciente/doctor, y validaciones de cédula, edad, género y campos obligatorios.

Código y configuración:

    src/test/java/com/bookly/backendcf/clinicalhistory/ClinicalHistoryPersistenceTest.java
    src/test/resources/application.properties

Para ejecutar una sola prueba:

    .\mvnw test "-Dtest=ClinicalHistoryPersistenceTest#registraYConsultaHistoriaPorCedula"

Reportes generados:

- `target/surefire-reports/` — resultado de cada prueba.
- `target/site/jacoco/index.html` — cobertura.

## Estructura

    src/main/java/com/bookly/backendcf/
    ├── BackendcfApplication.java
    └── clinicalhistory/
        ├── domain/
        │   ├── Patient.java
        │   ├── Doctor.java
        │   ├── ClinicalHistory.java
        │   ├── Gender.java
        │   ├── exception/
        │   └── validation/
        ├── application/
        │   ├── RegisterClinicalHistoryService.java
        │   ├── FindClinicalHistoriesService.java
        │   ├── command/
        │   └── port/out/
        ├── infrastructure/
        │   └── persistence/          (repositorios JPA)
        └── presentation/graphql/     (resolvers, pendiente)

    src/main/resources/
    ├── graphql/schema.graphqls
    └── db/migration/V1__create_clinical_history.sql

    src/test/
    ├── java/com/bookly/backendcf/clinicalhistory/ClinicalHistoryPersistenceTest.java
    └── resources/application.properties
