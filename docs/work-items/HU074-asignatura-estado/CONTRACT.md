# CONTRACT — HU074: Activar o inactivar asignaturas

Contrato HTTP congelado para el frontend de la misma rama. Fuente de BD: migración 58 (`usp_toggle_estado_asignatura`,
`usp_crear_grupo`, `usp_asociar_asignatura_plan`); resultsets canónicos sin cambio. Única firma que cambia (aditiva, ajuste de
la revisión independiente del 2026-11-19): `usp_toggle_estado_asignatura` suma `@estaActivaDeseada BIT = NULL` como último
parámetro.

## 1. `PATCH /api/v1/coordinador/asignaturas/{id}/estado` (COORDINADOR)

Cuerpo **opcional** (`CambiarEstadoAsignaturaRequest`):

| Cuerpo | Efecto | `@estaActivaDeseada` |
|---|---|---|
| sin cuerpo, `{}` o `{"estaActiva": null}` | alterna el estado (contrato anterior, compatible) | `NULL` |
| `{"estaActiva": true}` | la asignatura queda activa; si ya lo estaba, éxito sin cambio (`ASI_011`) | `1` |
| `{"estaActiva": false}` | la asignatura queda inactiva (sujeto a `ASI_009`); si ya lo estaba, éxito sin cambio (`ASI_012`) | `0` |
| `{"estaActiva": "x"}` (no booleano) | 400 (cuerpo ilegible), sin llamar a la BD | — |

Ejemplos:

```http
PATCH /api/v1/coordinador/asignaturas/7c1e…/estado
Content-Type: application/json

{"estaActiva": false}
```

```http
PATCH /api/v1/coordinador/asignaturas/7c1e…/estado
```
(sin cuerpo: alterna)

El frontend debería enviar el estado que muestra la pantalla como deseado (`estaActiva: !estadoMostrado`): así un doble clic o
una pantalla desactualizada no invierten el estado a ciegas.

### 200 (cambio aditivo)

Antes: `{"exitoso": true, "datos": null}`. Ahora (`ApiDataMensajeResponse<EstadoAsignaturaResponse>`):

```json
{
  "exitoso": true,
  "datos": { "idAsignatura": "7c1e…", "estaActivaAsignatura": false },
  "mensajeUsuario": "La asignatura quedó inactiva."
}
```

- `datos.estaActivaAsignatura`: estado en que quedó la asignatura (mismo nombre que en la malla, `AsignaturaPlanDTO`). Sale del
  **resultado del SP**: código de éxito del canal técnico (`mensajeTecnicoResultado` = `DBCODE=ASI_011|…` → `true`,
  `DBCODE=ASI_012|…` → `false`). Si el SP no publica el código (SP anterior a la migración 58), el estado pedido y, si se pidió
  alternar, el contrario al leído en la validación de ámbito.
- `mensajeUsuario`: texto que entregó el SP (`ASI_011` "La asignatura quedó activa." / `ASI_012` "La asignatura quedó
  inactiva."). Si el SP lo entrega vacío: catálogo de mensajes por código y, sin catálogo, el respaldo de
  `MensajeEstadoAsignatura`.

### Errores

| HTTP | `code` | `message` (catálogo; respaldo del enum) | Cuándo |
|---|---|---|---|
| 409 | `ASI_009` | No puedes inactivar la asignatura: tiene grupos en el período vigente o en períodos futuros. | Inactivar con grupos en un período no cerrado (vigente o futuro), en cualquier programa |
| 409 | `ASI_006` | La asignatura es compartida con programas que no coordinas; solo puedes cambiar su semestre y su carácter en tu plan. | La asignatura tiene vínculo activo con un programa que el coordinador no coordina |
| 403 | `FORBIDDEN` (`ACC_APP_006` / `ACC_APP_009`) | (sin cambio) | Coordinador sin programa o asignatura fuera de su programa |

Reactivar (inactiva → activa) siempre se permite tras las validaciones de ámbito.

## 2. Asignatura inactiva: `ASI_010` (409)

| Endpoint | SP | Rol |
|---|---|---|
| `POST /api/v1/grupos` | `usp_crear_grupo` | COORDINADOR (el DOCENTE recibe 403 en el filtro, DEC-065 (4)) |
| `POST /api/v1/coordinador/planes-estudio/{planId}/asignaturas-asociadas` | `usp_asociar_asignatura_plan` | COORDINADOR |

Cuerpo de error (`ApiErrorResponse`):

```json
{
  "timestamp": "2026-11-19T10:00:00-05:00",
  "status": 409,
  "error": "Conflict",
  "code": "ASI_010",
  "message": "La asignatura está inactiva; actívala para usarla.",
  "path": "/api/v1/grupos",
  "correlationId": "…"
}
```

La oferta de creación de grupo (`usp_consultar_oferta_creacion_grupo_docente`) excluye las asignaturas inactivas en la BD; el
backend no la consume. Si un SP de consulta lanzara `DBCODE=ASI_010` con `THROW`, `DbExceptionTranslator.translateDbCode` lo
traduce al mismo 409.

## 3. Cambio de `ASI_006`

`ASI_006` también lo emiten `usp_actualizar_asignatura` y el toggle. Antes se publicaba como `code: "CONFLICT"` con el texto
genérico "La operacion genera un conflicto con el estado actual."; ahora `code: "ASI_006"` con el texto del catálogo. El
estado HTTP (409) no cambia. El frontend muestra `message`; no depende de `code: "CONFLICT"` para este caso.

## 4. Deuda

`ASI_002`, `ASI_003` y `ASI_005` siguen como `CONFLICT` con el texto genérico (fuera del alcance de HU074). El frontend ya espera
su propio código (`coordinator-management.service.spec.ts`); conviene promoverlos igual que `ASI_006` en un cambio aparte.
