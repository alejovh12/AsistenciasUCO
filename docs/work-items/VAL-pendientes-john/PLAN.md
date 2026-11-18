# VAL-pendientes-john (backend) - PLAN

Rama `fix/VAL-pendientes-john` desde `origin/develop` 7d9c95cc. Filas de `informes/LISTA_CHEQUEO_VALIDACION_2026-11-12.md`:
PRU-15 (regla de período cerrado, decisión de John del 2026-11-18). PRU-13 es solo de BD (el plazo lo calcula la base) y SEG-11 no
se ejecutó (ver `VALIDATION.md`).

## Clase de cambio y alcance

Mapeo de errores de la BD (adapter de persistencia) y catálogo de mensajes de respaldo. Sin endpoints nuevos ni cambios de
request/response: la BD (migración 49, `johnjduque/gestion-asistencia-db`) responde dos códigos nuevos y el backend los publica con
el mismo mecanismo que `PER_007` (commit `52c45fcc`, luego movido a `MensajeUsuarioErrorCode` en `11c7d38c`).

| Código BD | Cuándo | HTTP | Mensaje |
| :--- | :--- | :--- | :--- |
| `PER_008` | Abrir toma o QR, lote por toma o por sesión, cerrar toma, registrar por token, radicar, resolver o retirar un reclamo de una sesión de un período ya cerrado por el cierre masivo | 409 `PER_008` | El de la BD (lleva el nombre del período); respaldo en `MensajeUsuarioErrorCode.PER_008` |
| `PER_009` | Segundo cierre masivo de un período ya cerrado (antes era un éxito idempotente con otra auditoría) | 409 `PER_009` | El de la BD (lleva la fecha local del primer cierre) |

Archivos: `MensajeUsuarioErrorCode` (constantes `PER_008`, `PER_009`), `DbFailureClassifier` (casos), `DbExceptionTranslator`
(`CONFLICT_CODES` y códigos con mensaje de la BD), fixture `src/test/resources/messageucolab/catalogo-mensajes-usuario.psv` (+2 filas
con el formato de `build-message-catalog.py`) y su conteo en `MessageUcoLabCatalogoTest` (489).

Pruebas: `DbFailureClassifierPeriodoAcademicoTest` (PER_008 en las siete operaciones que la BD rechaza; PER_009 en el cierre).

No se cambió `EjecutarCierreMasivoUseCaseImpl`: para un período ya cerrado el evaluador de alertas sigue corriendo antes del SP
(como hasta hoy en un segundo cierre); con las matrículas ya finalizadas no tiene grupos pendientes que avisar, y el SP responde
`PER_009`. Si se quiere evitar esa llamada, la vista `uv_periodo_academico` tendría que exponer `fechaCierreUtc` (pendiente, no
pedido).

Frontend: muestra el mensaje del backend con el mecanismo genérico (`getApiErrorMessage`); no necesita cambio.

Riesgo y reversa: revertir el commit; con la BD sin la migración 49 los códigos nuevos nunca llegan.
