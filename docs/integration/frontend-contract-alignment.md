# Frontend contract alignment

Este documento registra contratos backend que deben revisarse durante la alineacion del frontend. El frontend no se modifica en esta fase.

## Errores

El backend devuelve `message` y `details[]`. No devuelve `mensajeUsuario` en el contrato de errores. El frontend debe alinearse a `error.message` y `error.details` cuando correspondan.

## Identificacion

`numeroIdentificacion` debe enviarse actualmente como JSON NUMBER compatible con `Integer`, no como String. Esto es temporal hasta la futura migracion DB a `String` / `VARCHAR`.

## Asistencia

El backend actual espera conceptualmente `estudiante`, `grupo`, `sesion`, `presente` y `observacion`. El frontend previamente revisado tenia un modelo batch basado en `sesion` y `asistenciaJSON`. El contrato definitivo debe decidirse coordinado con la futura implementacion real de asistencia SQL.

## Revision de asistencia

El backend espera `asistencia` y `motivo`. El frontend previamente utilizaba algo equivalente a `asistencia` y `observacion`. Debe alinearse en una etapa frontend/funcional futura.

## Queries canonicas

- `GET /api/v1/docentes/{docenteId}`
- `GET /api/v1/docentes/{docenteId}/asignaciones`
- `GET /api/v1/sesiones/{sesionId}`
- `GET /api/v1/grupos/{grupoId}/asistencias`

Los POST legacy de consulta siguen disponibles temporalmente. Durante la alineacion frontend se debe migrar hacia los GET canonicos.
