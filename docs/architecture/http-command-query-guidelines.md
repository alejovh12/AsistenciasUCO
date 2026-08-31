# Guia HTTP Command / Query

AsistenciasUCO modela las operaciones HTTP segun su intencion de negocio:

- QUERY: usa `GET`, no modifica estado y recibe datos por `PathVariable` o `RequestParam`.
- COMMAND: usa `POST`, representa una accion de negocio que modifica estado.

PUT, PATCH y DELETE no estan obsoletos. Esta es una decision de diseño de AsistenciasUCO, no una limitacion ni deprecacion del estandar HTTP.

Rutas canonicas de consulta:

- `GET /api/v1/docentes/{docenteId}`
- `GET /api/v1/docentes/{docenteId}/asignaciones`
- `GET /api/v1/sesiones/{sesionId}`
- `GET /api/v1/grupos/{grupoId}/asistencias`

Los POST de consulta anteriores se conservan temporalmente por compatibilidad. Se eliminaran cuando el frontend consuma las rutas GET canonicas.

CORS habilita GET, POST y OPTIONS, que corresponden a los metodos utilizados actualmente por la API.

Los parametros booleanos HTTP utilizan representacion estricta exacta `true`/`false`.
