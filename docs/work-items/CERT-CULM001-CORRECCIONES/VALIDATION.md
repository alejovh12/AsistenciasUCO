---
status: active
type: work-item
scope: backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-10
---

# VALIDATION — CERT-CULM001-CORRECCIONES

Corridas locales del 2026-11-10 (madrugada), sin cambios de BD.

## Corrida de las correcciones, antes de la recertificación (01:24)

| Capa | Comando | Resultado |
|---|---|---|
| Backend | `.\mvnw.cmd -B -o clean verify` (Java 25) | `BUILD SUCCESS`: 4194 tests, 0 fallos, 0 errores, 0 omitidos; "All coverage checks have been met" |
| Frontend | Pasos de `npm run verify`, uno por uno | 2664 de 2664 SUCCESS; build de producción exit 0. El detalle está en el `VALIDATION.md` del frontend |
| BD | No aplica | Las correcciones no tocan la BD |
| PDP del lab | No aplica | `access-catalog` y `SecurityConfig` no cambian |

Nota sobre el build: la primera corrida fue `verify` sin `clean` y falló `SolicitudesIngresoRetiradasTest`, porque en `target/classes` quedaban tres plantillas `SOLICITUD_INGRESO_*` de un build anterior a CULM-001 A. No están en `src/`. Con `clean verify` pasa todo; el jar con el que se probó en pantalla sale de esa corrida limpia.

## Corrida con las correcciones de la recertificación (03:24)

| Capa | Comando | Resultado |
|---|---|---|
| Backend | `.\mvnw.cmd -B -o clean verify` sobre el árbol de trabajo, que también tiene HU018/HU019 | `BUILD SUCCESS`: 4221 tests, 0 fallos, 0 errores, 0 omitidos; "All coverage checks have been met". De las 27 pruebas nuevas, 4 son de este work item (`MessageCatalogJpaMarcadoresSecuencialesTest`) |
| Frontend | `test:ci` con solo las correcciones (02:15) | 2675 de 2675 SUCCESS; statements 93.03 %, lines 94.02 % |

## Comprobación del motor de alertas (HU026C)

- Antes de la corrección: a las 00:48 el log del backend ya tenía 133 líneas `SQL operation failed. operation=evaluarAlertasInasistenciaGrupo` (una por minuto desde el arranque de las 22:40 del 2026-11-09), y las tablas `AlertaInasistencia`, `RevisionCancelacion` y `EvaluacionAlertaGrupo` estaban vacías.
- Después: el backend arrancó con el jar nuevo a las 01:26 y, en el primer minuto, el job creó 2 alertas vigentes de Carlos Zapata (ADVERTENCIA con umbral 15 % y PERDIDA con umbral 20 %, las dos con 53.47 % de inasistencia: 2.66 h de falta sobre 4.97 h dictadas), 1 revisión de cancelación en `PENDIENTE_DOCENTE`, 1 fila en `EvaluacionAlertaGrupo` y 1 en `RevisionCancelacionHistorial`. El log no volvió a registrar el fallo.

## Re-prueba en pantalla (01:33–02:04)

Con los usuarios de prueba del lab, la BD local y el backend con el jar de las correcciones. El detalle está fuera de los repos, en `informes/CERTIFICACION_FASE3_CULM001_2026-11-10/RECERTIFICACION.md`.

| Ítem | Resultado |
|---|---|
| HU007 | **PASA.** "Mis materias" tiene el buscador: filtra por nombre, código, grupo y docente sin importar tildes ni mayúsculas |
| HU026C | **PASA** el flujo completo: el estudiante ve sus 2 alertas, el docente ve la marca y propone la cancelación, el coordinador ve la revisión y la rechaza, y el estudiante sigue activo. Aparecieron N1 (texto con `{}`) y N2 (enlace a un 404), ya corregidos |
| HU171 | **PASA.** La carga CSV del coordinador crea al docente: "1 creadas, 0 ya existentes, 1 con error" |
| Varias tomas | **PASA.** Con tomas SJC y AN el estudiante ve PARCIAL, puede radicar el reclamo (202) y queda pendiente |
| HU042 | **PASA.** El docente aprueba el reclamo; el estudiante lo ve aprobado con la respuesta; la sesión queda JUSTIFICADA; hay fila en `AuditoriaEvento` y aviso `RECLAMO_ASISTENCIA_RESUELTO` |
| HU156 | **Parcial.** La admisión responde 201 y los errores se ven en pantalla. Aparecieron N4 (la clave inicial no se pedía sola, ya corregido) y N3 (el admitido sin grupo no aparece en la lista: decide John) |

## Segunda pasada en pantalla (03:26–03:43)

Con el jar de la corrida de las 03:24. El detalle está en la sección "Segunda pasada" del mismo reporte.

| Hallazgo | Resultado |
|---|---|
| N1, texto de las alertas | **PASA.** "Llevas 53.47% de inasistencia en Arquitectura de Software. Si llegas al 20.00% podrías perder la asignatura." y "Alcanzaste 53.47% de inasistencia en Arquitectura de Software (límite 20.00%). Tu docente revisará tu caso.", iguales al `mensaje` de `GET /api/v1/estudiante/alertas` |
| N2, "Ver detalle de la materia" | **PASA.** Abre `/app/estudiante/mis-materias/materias?materia=…&grupo=…` con el detalle y sus 8 sesiones. Sin 404 |
| N4, clave inicial | **PASA.** `POST /api/v1/admin/estudiantes/admision` → 400 `ERR_PASSWORD_REQUERIDO` y el formulario abre solo "Contraseña inicial requerida" con "La clave es obligatoria." junto al campo. Se canceló: no se creó nadie |
| Documento inválido | **PASA.** Con `12a4` el campo dice "Escribe solo números, sin puntos, espacios ni ceros al inicio (máximo 2147483647)." y el botón sigue deshabilitado |

Observación para el equipo de CULM-001, sin cambio: el texto de una alerta usa el porcentaje del momento en que se cruzó el umbral (53.47 %), y la misma tarjeta muestra el porcentaje actual (38.12 % después de aprobar un reclamo). Las dos cifras son correctas, pero juntas pueden confundir.
