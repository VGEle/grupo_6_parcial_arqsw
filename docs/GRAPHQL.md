# API GraphQL de historias clínicas

Esta API permite registrar historias clínicas y consultar el historial de un
paciente mediante su número de identificación. La API devuelve respuestas JSON
a través de GraphQL y utiliza datos ficticios para las pruebas.

## Acceso local

Cuando la aplicación se ejecuta con Docker:

- Endpoint GraphQL: `http://localhost:8080/graphql`
- GraphiQL: `http://localhost:8080/graphiql`
- Esquema GraphQL: `src/main/resources/graphql/schema.graphqls`

La petición HTTP debe utilizar el método `POST` y el cuerpo debe contener una
consulta GraphQL y, opcionalmente, sus variables.

## Mutation `createClinicalHistory`

Registra una historia clínica junto con los datos del paciente y del médico.

### Consulta

```graphql
mutation CreateClinicalHistory($input: CreateClinicalHistoryInput!) {
  createClinicalHistory(input: $input) {
    id
    patient {
      identification
      firstName
      lastName
      age
      gender
    }
    doctor {
      name
      professionalLicense
      specialty
    }
    consultationReason
    diagnosis
    treatment
    date
  }
}
```

### Variables

```json
{
  "input": {
    "patient": {
      "identification": "1000000001",
      "firstName": "Ana",
      "lastName": "Gómez",
      "age": 30,
      "gender": "FEMALE"
    },
    "doctor": {
      "name": "Carlos Pérez",
      "professionalLicense": "MED-1001",
      "specialty": "Medicina general"
    },
    "history": {
      "consultationReason": "Dolor de cabeza",
      "diagnosis": "Cefalea tensional",
      "treatment": "Reposo e hidratación",
      "date": "2026-11-27T14:00:00Z"
    }
  }
}
```

### Respuesta exitosa

```json
{
  "data": {
    "createClinicalHistory": {
      "id": "<generated-id>",
      "patient": {
        "identification": "1000000001",
        "firstName": "Ana",
        "lastName": "Gómez",
        "age": 30,
        "gender": "FEMALE"
      },
      "doctor": {
        "name": "Carlos Pérez",
        "professionalLicense": "MED-1001",
        "specialty": "Medicina general"
      },
      "consultationReason": "Dolor de cabeza",
      "diagnosis": "Cefalea tensional",
      "treatment": "Reposo e hidratación",
      "date": "2026-11-27T14:00:00Z"
    }
  }
}
```

## Query `clinicalHistoriesByIdentification`

Consulta las historias clínicas asociadas a la identificación de un paciente.

### Consulta sin paginación explícita

Si no se envía `first`, el tamaño predeterminado de página es 10.

```graphql
query ClinicalHistoriesByIdentification($identification: String!) {
  clinicalHistoriesByIdentification(
    identification: $identification
  ) {
    nodes {
      id
      patient {
        identification
        firstName
        lastName
        age
        gender
      }
      doctor {
        name
        professionalLicense
        specialty
      }
      consultationReason
      diagnosis
      treatment
      date
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

### Variables

```json
{
  "identification": "1000000001"
}
```

### Consulta paginada

```graphql
query ClinicalHistoriesPage(
  $identification: String!
  $first: Int!
  $after: String
) {
  clinicalHistoriesByIdentification(
    identification: $identification
    first: $first
    after: $after
  ) {
    nodes {
      id
      consultationReason
      diagnosis
      treatment
      date
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

Primera página:

```json
{
  "identification": "1000000001",
  "first": 10,
  "after": null
}
```

Para solicitar la siguiente página, se debe enviar el valor de `endCursor`
obtenido en la respuesta anterior:

```json
{
  "identification": "1000000001",
  "first": 10,
  "after": "<endCursor-de-la-respuesta-anterior>"
}
```

El tamaño máximo permitido para `first` es 50 registros. El cursor codifica la
fecha de consulta y el ID del último registro de la página anterior, por lo que
permite continuar con un orden estable incluso cuando varias historias tienen
la misma fecha.

### Respuesta exitosa

```json
{
  "data": {
    "clinicalHistoriesByIdentification": {
      "nodes": [
        {
          "id": "<generated-id>",
          "consultationReason": "Dolor de cabeza",
          "diagnosis": "Cefalea tensional",
          "treatment": "Reposo e hidratación",
          "date": "2026-11-27T14:00:00Z"
        }
      ],
      "pageInfo": {
        "hasNextPage": false,
        "endCursor": null
      }
    }
  }
}
```

## Validaciones y errores

La API valida los datos antes de guardar una historia clínica. Entre las
validaciones esperadas se encuentran:

- Identificación obligatoria y con formato válido.
- Edad dentro del rango permitido.
- Género válido: `MALE`, `FEMALE` u `OTHER`.
- Campos obligatorios del paciente, médico e historia.
- Identificación existente para las consultas.
- `first` entre 1 y 50.
- Cursor válido cuando se utiliza `after`.

GraphQL devuelve los errores en el campo `errors`. Ejemplo:

```json
{
  "errors": [
    {
      "message": "No existe un paciente con cédula: 9999999999"
    }
  ],
  "data": null
}
```

## Prueba con `curl`

Ejemplo de consulta directa contra el endpoint:

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "query($identification: String!) { clinicalHistoriesByIdentification(identification: $identification) { nodes { id diagnosis date } pageInfo { hasNextPage endCursor } } }",
    "variables": { "identification": "1000000001" }
  }'
```

## Evidencias complementarias

Las evidencias de GraphiQL, Redis, Prometheus y Grafana están organizadas en
`docs/EVIDENCE.md` y en la carpeta `docs/evidence/`. Todas deben ejecutarse con
datos ficticios después de levantar la aplicación con Docker.

