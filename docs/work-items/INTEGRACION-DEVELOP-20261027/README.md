---
status: active
type: work-item
scope: fullstack-integration
owner: backend-team
last-reviewed: 2026-10-27
---

# INTEGRACION-DEVELOP-20261027 — sincronización con `develop`, HU051/HU053–HU055, PDP y hallazgos del cierre

Estados usados: `PASS`, `FAIL`, `BLOCKED`, `NOT_RUN`. Nada se marca `PASS` sin la evidencia citada. La evidencia cruda
(logs, JSON, bundles) vive fuera de Git en `C:\Users\josev\AsistenciasUCO-backups-20261027`.

La consolidación local y la sincronización posterior al `develop` actualizado están documentadas en
[`CONSOLIDACION-LOCAL-2026-10-27.md`](CONSOLIDACION-LOCAL-2026-10-27.md). Ese informe supersede los SHA y conteos
históricos de este README donde difieran; los pendientes funcionales y de cierre permanecen vigentes.

## 1. Estado inicial y final de los repositorios

| Repo (remoto personal) | Rama inicial / HEAD | Cambios locales | `develop` descargado | Integración |
|---|---|---|---|---|
| Backend `alejovh12/AsistenciasUCO` | `codex/security-baseline-017-integration` @ `a43c7263` (ancestro de develop, 60 commits atrás) | 1 doc modificado (`LOCAL-STABILIZATION-LB004D…/VALIDATION.md`) | `84ac59d5` | `codex/integracion-develop-20261027` = develop (fast-forward limpio) |
| Frontend `shesho22/AsistenciasUCO-Frontend` (remoto `legacy-frontend`) | `feat/security-baseline-017-frontend` @ `8b85b45` (33 atrás) | 8 modificados + `src/app/features/security/` sin seguimiento | `f010db3d` | `codex/integracion-develop-20261027` = develop + `fe37882` (Seguridad UCO recuperada; 1 conflicto de menú resuelto conservando ambas entradas) |
| DB `johnjduque/gestion-asistencia-db` | `main` @ `25cf2e8` limpio | — | `fec4503c` | `codex/integracion-develop-20261027` = develop |

Los SHA coinciden con los verificados en el expediente. Los remotos `org`/`origin` de la organización no se usaron (solo
lectura de referencias personales; ningún push). Copias preventivas: bundle `--all`, parches y rama `snapshot/20261027-*`
por repo (no publicadas); `git stash` del frontend conservado (no se hizo `drop`). No hubo `reset --hard`, `clean`,
`force-push` ni borrado de ramas.

## 2. HU051 y HU053–HU055 (código integrado de develop)

| Ruta | Rol | Catálogo | PDP tras reprovisionar | Ownership |
|---|---|---|---|---|
| `POST /api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro` (HU051) | DOCENTE | sí | ALLOW DOCENTE / DENY resto | docente titular (SP `usp_retirar_estudiante_grupo`) |
| `POST /api/v1/estudiante/solicitudes-matricula` (HU053, ya existía en catálogo) | ESTUDIANTE | sí | ALLOW ESTUDIANTE | estudiante autenticado |
| `GET /api/v1/estudiante/solicitudes-matricula` (HU054) | ESTUDIANTE | nueva | ALLOW ESTUDIANTE | propias |
| `POST /api/v1/estudiante/solicitudes-matricula/{id}/retiro` (HU054) | ESTUDIANTE | nueva | ALLOW ESTUDIANTE | propias |
| `GET /api/v1/docente/grupos/{grupoId}/solicitudes-ingreso` (HU055) | DOCENTE | nueva | ALLOW DOCENTE | titular |
| `PATCH /api/v1/docente/solicitudes-ingreso/{id}` (HU055) | DOCENTE | nueva | ALLOW DOCENTE | titular |

Nota: “5 rutas” = 1 de retiro + 4 nuevas de solicitudes; `POST …/solicitudes-matricula` ya estaba en el catálogo. Además
develop añadió **10 filas de indicadores** (3 admin, 4 coordinador, 3 decano) que también faltaban en el PDP.

Compatibilidad: el servicio Angular `solicitud-ingreso.service.ts` y `group.service.ts` usan exactamente estas rutas y los
campos de `CONTRACT.md` (PASS por lectura; el flujo visual en navegador es `NOT_RUN`). TD-066: `NotificacionPort` solo
escribe en log (`LogNotificacionAdapter`); no hay correo/push/UI.

## 3. Hallazgo nuevo de integración (verificado contra SQL Server real)

Las pruebas unitarias de develop mockean el `EntityManager` con `UUID` y no detectaron:

| ID | Hallazgo | Estado |
|---|---|---|
| BUG-HU-02 | `GET /estudiante/solicitudes-matricula` y `GET /docente/grupos/{id}/solicitudes-ingreso` respondían **500** (`ClassCastException String→UUID`); el aviso de destinatarios de HU051 y HU053–055 fallaba en silencio (best-effort). | **FIXED** en `codex/fix-hu-guid-mapping-20261027` (RED `80ccebb0`, fix `af31fcaa`), E2E 62/62 |
| INT-HU-01 | Tras HU051 la matrícula queda `I`; HU053 permite volver a solicitar (solo bloquea `A`) pero HU055 `ACEPTAR` falla **409 `ERR_MATRICULA_DUPLICADA`** porque `usp_validar_registro_estudiante_en_grupo_interno` rechaza cualquier fila previa (`uv_estudiante_grupo`). La solicitud queda pendiente para siempre salvo rechazo. | **OPEN – DECISIÓN de Jhon (DB)**: reactivar la fila `I` al aceptar, o bloquear la solicitud (SIG_004) si existe cualquier fila previa |
| DB-GRANTS | 32 SP que el backend invoca no tenían `GRANT EXECUTE` para `rol_asistencias_runtime` (KPIs decano/coordinador/admin, directorio de docentes del programa, catálogos de áreas/facultades/programas/coordinadores…): con el login runtime real → **500 `EXECUTE permission was denied`**. | **FIXED** en DB `codex/fix-grants-runtime-20261027` (`3015558`, prueba RED→GREEN) |

## 4. Reprovisionamiento PDP (incremental, sin `--prune`)

| Paso | Resultado |
|---|---|
| `provision_asistencias.py catalog` | PASS: 154 endpoints, 0 sin regla, 0 obsoletos |
| `verify` antes | 15 recursos y 22 concesiones faltantes; 0 concesiones sobrantes |
| `pdp` (sin `--prune`) | PASS: 15 recursos + 22 concesiones agregadas, 0 revocadas; no rotó credencial |
| `verify` después | PASS `clean: true` |
| Asignaciones de usuarios antes/después (`pdp_before.json`/`pdp_after.json`) | PASS: asignaciones de rol y de perfil, y roles de cada perfil, **idénticos**; 0 concesiones quitadas |
| Decisiones reales PEP→PDP (`verify-decisions` con renovación de token) | PASS: 949 decisiones, 0 fallos (el `verify-decisions` oficial falla por expiración del token tras ~5 min: ver §9) |

No se ejecutó `users` ni `--prune`; la asignación de roles/perfiles a personas sigue **manual** en Seguridad UCO.

## 5. Base de datos

| Elemento | BD viva (1433, login `uco_dev_runtime`) | BD desechable (14333) |
|---|---|---|
| Tabla `SolicitudIngresoGrupo` (+ UX filtrado, índice, 5 FK, estados `E`/`X`) | **PENDIENTE** (no visible/ausente) | instalada |
| 8 SP de HU051/HU053–055 + mensajes `RET_*`/`SIG_*` + grants | **PENDIENTE** | instalados |
| Suite DB (`test_summary.ps1`) | — | PASS: 421 `TEST_PASS`, `FAILED=0`, `DB GATE PASS` (incluye R-01..R-09, S-01..S-12) |
| Idempotencia del paquete A (15 archivos reaplicados) | — | PASS: 0 fallos |

La BD viva **no se modificó**. El login runtime solo ve 1 tabla, así que el inventario completo exige `sa`.
`live-db/aplicar-paquete-a.ps1` (inventario → respaldo `COPY_ONLY` + `RESTORE VERIFYONLY` + SHA-256 → aplicar 15 archivos →
verificar) lo ejecuta **el usuario**. No usa `deploy_schema.ps1` (re-sembraría usuarios/titularidades). Las FK nuevas salen de
`01_fk_solicitud_ingreso_grupo.sql` (solo desde la tabla nueva). El paquete B (KPIs, catálogos de áreas, HU010–011, cambios a
`Area`/`TipoPrograma`) es mayor y modifica tablas existentes: requiere revisión aparte de Jhon.

## 6. Pruebas

| Área | Comando / evidencia | Resultado |
|---|---|---|
| Backend develop | `mvnw -B verify` (84ac59d5) | PASS: 2588 tests, ArchUnit, JaCoCo gates; LINE 91.89 %, BRANCH 82.00 % |
| Backend fix GUID | `mvnw -B verify` en `af31fcaa` | PASS: 2591 tests (+3 RED→GREEN), ArchUnit, JaCoCo gates; LINE 91.88 %, BRANCH 81.98 % |
| Frontend (develop + Seguridad UCO) | `npm run test:ci` | PASS 1257/1257; líneas 91.07 % |
| Frontend con correcciones | `npm run test:ci` + `build --configuration=production` | PASS 1275/1275; líneas 91.72 %, ramas 74.69 %; build de producción PASS (rama `codex/fix-hallazgos-frontend-20261027` @ `ec4c25f`) |
| E2E HTTP HU051/HU053–055 (backend develop+fix → BD desechable, 5 usuarios reales Keycloak/PEP) | `e2e_hu_flows.py` | PASS 62/62 (antes del fix: 4 FAIL = 2 por 500 de BUG-HU-02, 1 por INT-HU-01 y 1 criterio de bandeja ajena que resultó ser lista vacía, no fuga). Ver §7 |
| Matriz GET por rol (355 sondas) | develop+fix, BD desechable | DECANO 26/26, COORDINADOR 35/35, ESTUDIANTE 15/15 alcanzan la lógica; 403 esperado 100 % |
| Preservación del laboratorio vivo | `preserve_check_live.py` | PASS: Ingeniería sigue con `PRUEBA INTEGRACION DECANO UCO` (`c512fb20…`) |
| KPIs de decano/coordinador/administrador con login de mínimo privilegio | 10 rutas, antes de los grants → 500 `EXECUTE permission was denied`; con `rol_asistencias_runtime_inventario_backend.sql` → 200 | PASS (BD desechable) |
| Estado del PDP tras reinicio de Docker (≈22:20) | `pdp_after_reboot.json` vs `pdp_after.json` | PASS: idéntico (el PDP salió con error por arrancar antes que Keycloak; se reinició con `docker start`) |
| SonarCloud / CI remoto / navegador | — | `NOT_RUN` (no hay push) |

## 7. Evidencia E2E de HU051 y HU053–HU055 (BD desechable)

Positivos: solicitar, consultar, rechazar con motivo, retirar solicitud abierta, aceptar (`ACEPTADA`), retirar estudiante con
motivo → matrícula `I`, historial conservado, cupo 29 liberado, evento `RETIRO_ESTUDIANTE_GRUPO` en `AuditoriaEvento`, avisos
por `LogNotificacionAdapter`. Negativos: duplicado 409, ya matriculado 409, grupo inexistente 404, cuerpo vacío 400, estado
inválido 400, rechazo sin motivo 400, doble resolución 409, retirar resuelta 409, docente no titular (404 / bandeja vacía),
reintento de retiro 404, estudiante inexistente 404; ADMIN/DECANO/COORDINADOR/ESTUDIANTE sobre rutas del docente → 403 y
DOCENTE sobre rutas del estudiante → 403. `EN_ESPERA` (grupo sin cupo): **NOT_RUN** en esta pasada (cubierto por S-0x del SQL).

## 8. Hallazgos del expediente

| ID | Causa raíz verificada | Estado |
|---|---|---|
| SEC-01 | Decisión: asignación PDP manual | DECISIÓN respetada; sin cambios |
| AUTH-02 | `dean-grupo-detalle` llamaba `/api/v2/sesiones/grupo/{id}` (solo DOCENTE) y mostraba el 403 como “0 sesiones” | FIXED (usa `/decano/sesiones` + filtro `idGrupo`, error visible) |
| AUTH-03, AUTH-04, INT-05 | **`role-scope.interceptor.ts` cortaba en el cliente (403 sintético, sin llegar al backend)** los GET que `SecurityConfig` y el catálogo sí conceden al DECANO (`coordinador/periodos-academicos`, `admin/sedes`, `admin/areas`…); Mi Facultad además pedía rutas solo-ADMIN (`espacios-fisicos`, `facultades`, `parametros`, `auditoria`) | FIXED (excepciones de lectura + Mi Facultad solo con rutas del decano) |
| DAT-06 | `planesDeLaEstructura` fija `totalCreditos/Asignaturas = 0` | FIXED (se calculan desde la malla; “—” si no carga) |
| DAT-07 | “Semestres” = filas `SemestrePlanEstudio` (1) vs semestre de la asignatura (8) | OPEN – DECISIÓN de contrato (DB/Frontend) |
| DAT-08 | El detalle inventaba “Aula Central” y cupo 30 | FIXED (sin valores inventados); la fuente real del aula no la entrega `/decano/grupos`: OPEN |
| UX-09 | Errores HTTP renderizados como vacío | FIXED en vistas del decano; auditoría de las demás vistas `NOT_RUN` |
| UX-10 | `<select>` enlazaba `[value]` antes de existir las `<option>` | FIXED (`PaginationComponent`, global) |
| UX-11 | Ficha sin foco/scroll | FIXED |
| REG-12 | No reproducido | NOT_RUN |
| DASH-13 | “Grupos en curso” sumaba por coordinador; periodo “2026-II” escrito a mano | FIXED (usa `gruposActivos`/`nombrePeriodoAcademico` de `/decano/indicadores`). “Docentes de Facultad” sigue siendo suma por coordinador: OPEN |
| DASH-14 | 94 % fijo | Ya resuelto en develop (`dean-indicators.service.ts`, prueba “no 94 %”) |
| GIT-15 | Divergencia/trabajo sin commit | RESUELTO (preservado e integrado) |
| ADM-16 | `admin-system`/`admin-facultades` llamaban `/coordinador/planes-estudio`; el backend responde 403 de negocio “No fue posible resolver el programa ni la facultad” (log del 27/10 00:34) | FIXED |
| ADM-17 | `GET /admin/auditoria`, `/admin/sedes`, `/admin/espacios-fisicos` → 501 `FEATURE_UNAVAILABLE` (stub intencional) | OPEN (funcionalidad pendiente; no es PASS) |
| LAB-18 | — | PASS verificado |
| DEC-ADM-01 | El catálogo/PDP permite ADMINISTRADOR en 22 rutas `/decano/**` y `/coordinador/**`, pero la capa de negocio siempre responde 403 (el admin no tiene facultad/programa) | OPEN – DECISIÓN: quitar ADMIN del catálogo (requiere `--prune`, no ejecutado) o definir alcance de admin |

## 9. Notas para el equipo

* `provision_asistencias.py verify-decisions` usa tokens obtenidos al inicio; las corridas de ~950 decisiones superan la
  vigencia y devuelven 401 falsos. Debería renovar el token ante 401 (como hizo el script de verificación de esta tarea).
* Bloqueo conocido del 26/10: la API admin del PDP autentica pero no autoriza (`PDP_ADMIN_API_AUTHORIZATION_FINDING.md`).
* Ramas antiguas: ver `ramas_clasificacion.md` (clase A = integrada y sin trabajo único; ninguna se elimina sin aprobación).

## 10. Decisiones y autorizaciones pendientes

1. Aplicar el paquete A en la BD viva (`live-db/aplicar-paquete-a.ps1`, lo ejecuta el usuario con `sa`).
2. Publicar ramas y abrir PRs a los `develop` personales (ver PRs listos en el informe final).
3. INT-HU-01 y DEC-ADM-01 (decisiones de producto/seguridad).
4. Eliminación de ramas clase A (solo con aprobación expresa).
