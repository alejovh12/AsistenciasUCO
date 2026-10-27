---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# PLAN — HU051: el docente titular retira a un estudiante de su grupo

## 1. Identidad y objetivo

- **Historia:** HU051 "como Docente necesito poder retirar a un estudiante de un grupo o clase".
- **Work Item ID:** `HU051-retirar-estudiante-grupo`
- **Fecha:** 2026-10-27
- **Base Git:** `develop` (DB `6fd5413`, backend `0569311`, frontend `73d1119`); rama `feature/HU051-retirar-estudiante-grupo`.
- **Clase de cambio:** funcional aditivo (nuevo comando + nueva lectura). Ninguna tabla cambia.

### Reglas decididas por John (2026-10-27)

| # | Regla | Decisión |
|---|---|---|
| R1 | Estado de la matrícula del retirado | Estado existente **"inactivo" (I)** de `EstadoEstudianteGrupo` (no se crea estado nuevo) |
| R2 | ¿Se puede retirar con asistencias registradas? | **Sí**; las asistencias se conservan como historial |
| R3 | Motivo | **Obligatorio** (1..500 caracteres), queda en la auditoría y va en el aviso |
| R4 | Período terminado | **Se bloquea** el retiro si `PeriodoAcademico.fechaFin` es anterior a hoy (fecha local, parámetro `TIEMPO.ZONA_HORARIA_SQLSERVER`) |
| R5 | Aviso de baja | Al **estudiante**, al **docente del grupo** y al **coordinador del programa** por `NotificacionPort` |
| R6 | Quién retira | Solo el **docente titular** del grupo |

### AS-IS comprobado

| Hecho | Evidencia |
|---|---|
| No existe SP de retiro; `DELETE /api/v1/coordinador/grupos/{grupoId}/estudiantes/{estudianteId}` responde 501 | `CoordinadorPortalController#retirarEstudianteDeGrupo` |
| El cupo es `Grupo.cantidadEstudiantes` (capacidad) menos matrículas activas | `uv_grupo.cuposDisponibles`, `usp_validar_cupo_disponible_grupo_interno` |
| No existe un puerto de notificaciones; el componente institucional (correo/WhatsApp/SMS) aún no está lanzado | búsqueda en `src/main` |
| `/api/v1/docente/**` ya exige `DOCENTE` en `SecurityConfig` | `SecurityConfig` línea `requestMatchers("/api/v1/docente/**")` |

### Criterios de aceptación

1. El docente titular retira a un estudiante con matrícula activa: la matrícula pasa a "inactivo", se libera el cupo, se conservan las asistencias y queda un `AuditoriaEvento` con el motivo.
2. Otro docente, un coordinador o un estudiante no pueden retirar (404 indistinguible de grupo inexistente); un usuario sin docente institucional recibe 403.
3. Motivo vacío/espacios/mayor a 500 ⇒ 400; período terminado ⇒ 409; estudiante sin matrícula activa ⇒ 404.
4. Tras el retiro se envía un aviso a estudiante, docente y coordinador; un fallo del aviso no revierte ni falla el retiro.
5. El docente ve en el padrón el botón "Retirar" solo para matrículas activas y un modal que exige el motivo.

## 2. Alcance

| Capa | Cambio | Archivos |
|---|---|---|
| DB | SP de comando `usp_retirar_estudiante_grupo` (resultset canónico), SP de lectura `usp_consultar_destinatarios_retiro_estudiante_grupo`, mensajes `RET_001..RET_005`, permisos aditivos, prueba | `schema/stored-procedures/` (2 NUEVOS), `schema/seed/21_mensajes_retiro_estudiante_grupo.sql`, `schema/security/rol_asistencias_runtime_hu051.sql`, `test/test_usp_retirar_estudiante_grupo.sql`, `test_suite.sql`, `test/test_contract_closure.sql` |
| Backend | Feature `docente/retirarestudiantegrupo`; `RetiroEstudianteGrupoPort` + `RetiroEstudianteGrupoJpaRepository`; **`NotificacionPort`** + `LogNotificacionAdapter` (provider temporal) + `NotificacionAdapterConfiguration`; `DocenteGrupoEstudianteController` (`POST /api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro`); mapeo `RET_*` en `DbFailureClassifier`; fila nueva en `infra/security-baseline/access-catalog.json`; conteo de repositorios 45 → 46 | NUEVOS + `DocenteWiringConfiguration`, `DbFailureClassifier`, `access-catalog.json` |
| Frontend | `GroupService.retirarEstudiante`; `TeacherRetiroEstudianteModalComponent`; botón "Retirar" y estado de la matrícula en `TeacherGrupoEstudiantesComponent`; evento por el hub; recarga del padrón; mock | `core/services/group.service.ts`, `features/teacher/teacher-grupos/**`, `mock-backend.interceptor.ts` |

**No alcance:** cambios a tablas o a objetos congelados, `SecurityConfig` (la regla `/api/v1/docente/**` ya cubre la ruta), el retiro por coordinador (`DELETE /coordinador/...` sigue 501), el envío real por correo/WhatsApp/SMS (componente no lanzado), el repo Infra.

## 3. Consumidores, seguridad y observabilidad

- Consumidor: pantalla de grupos del docente.
- Seguridad: `/api/v1/docente/**` (DOCENTE) + PEP del Security Baseline con la fila nueva del catálogo + Application exige docente institucional + titularidad en SQL (`Grupo.docente → Docente.usuario`).
- Datos personales: el correo de los destinatarios solo viaja al `NotificacionPort`; no se expone por HTTP ni se escribe en logs (el adaptador de log registra solo rol e id de usuario).
- Observabilidad: correlación propagada a ambos SP y a `AuditoriaEvento.correlationId`.

## 4. Decisiones

- **D1:** Retiro = cambio de estado de `EstudianteGrupo` (A → I); no se borran filas, así las asistencias y reportes conservan el historial (R1, R2).
- **D2:** La auditoría del motivo vive en `dbo.AuditoriaEvento` (tabla existente) dentro de la misma transacción del retiro; no se agregan columnas.
- **D3:** Los destinatarios se consultan **después** del retiro con un SP de lectura propio (no exige matrícula activa) para no ampliar el resultset canónico del comando.
- **D4:** `NotificacionPort` genérico y reutilizable (HU053-055, HU038). El provider por defecto es `LogNotificacionAdapter` con `@ConditionalOnMissingBean`, para que el componente real lo reemplace sin tocar los casos de uso.
- **D5:** Controlador propio `DocenteGrupoEstudianteController` para no ampliar el constructor de `DocentePortalController`.

## 5. Riesgos, rollback y parada

- **Riesgo PDP:** la fila nueva del `access-catalog.json` debe replicarse en el PDP del Security Baseline (`provision_asistencias.py`) para que el PEP autorice la ruta en modo baseline. **Coordinar con Alejo** al integrar.
- **Rollback:** revertir los commits; `DROP PROCEDURE` de los 2 SP, `REVOKE` de permisos, borrar la fila del catálogo. Sin cambios de datos estructurales.
- **Parada:** no iniciar HU053-055 en esta rama.

## 6. DoR

READY: reglas R1-R6 confirmadas por John el 2026-10-27; sin tablas nuevas; objetos congelados intactos.
