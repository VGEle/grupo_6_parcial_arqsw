# Arquitectura

La solución sigue una arquitectura por capas dentro del módulo clinicalhistory:

- domain: entidades y reglas del dominio.
- application: casos de uso de consulta y registro.
- infrastructure: JPA, PostgreSQL y Redis.
- presentation/graphql: schema, queries, mutations y mapeo de respuestas.

Componentes:

    Cliente GraphiQL
          |
          v
    Spring GraphQL
       /             v         v
    Redis   PostgreSQL
                |
                v
      Actuator -> Prometheus -> Grafana

La aplicación expone GraphQL por el puerto 8080 y Actuator por el puerto 8081.

