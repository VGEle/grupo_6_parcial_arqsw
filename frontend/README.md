# Frontend básico de Historias Clínicas

UI web estática (HTML + CSS + JavaScript puro) que consume la API GraphQL del
proyecto. Se encuentra en la carpeta `frontend/` del repositorio.

## Requisitos previos

1. Backend levantado:

Desde la raíz del repositorio:

```powershell
docker compose up -d --build
```

2. Python 3 (ya instalado). No necesita Node ni dependencias extra.

## Ejecutar

```powershell
cd frontend
python server.py
```

Abrir **http://localhost:5500**

El servidor hace dos cosas:

- Sirve `index.html`, `styles.css` y `app.js`.
- Redirige `POST /graphql` a `http://localhost:8080/graphql` (proxy), evitando
  problemas de CORS.

Si el backend está en otra dirección:

```powershell
$env:GRAPHQL_BACKEND = "http://localhost:8080/graphql"
$env:PORT = "5500"
python server.py
```

## Funcionalidades (requerimientos)

1. **Consulta por cédula** — formulario con cédula y tamaño de página; muestra
   paciente, doctor, motivo, diagnóstico, tratamiento y fecha, con botón
   *Cargar más* usando el cursor `after` devuelto por la API.
2. **Registro** — formulario con paciente (nombre, apellido, cédula, edad,
   género), doctor (nombre, cédula profesional, especialidad) e historia
   (motivo, diagnóstico, tratamiento, fecha). Al registrar, se consulta
   automáticamente la cédula para ver el resultado.
3. **Base de datos** — el frontend no crea la BD: consume la BD relacional ya
   implementada en el backend (`patients`, `doctors`, `clinical_histories` con
   sus relaciones 1:N y la migración Flyway `V1`).

## Validaciones en la UI

- Cédula: 4–30 caracteres alfanuméricos o guiones.
- Edad: 0–150.
- Campos obligatorios del formulario.
- Errores de GraphQL se muestran en pantalla.

## Archivos

```
frontend/
├── index.html   -> estructura de la UI
├── styles.css   -> estilos
├── app.js       -> queries/mutations y renderizado
├── server.py    -> servidor estático + proxy /graphql
└── README.md
```
